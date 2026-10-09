package main

import (
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestLabListenAddressScope(t *testing.T) {
	for _, address := range []string{"127.0.0.1:18766", "192.168.8.95:18766"} {
		if !validLabListenAddress(address) {
			t.Errorf("rejected private listener %q", address)
		}
	}
	for _, address := range []string{"0.0.0.0:18766", ":18766", "8.8.8.8:18766", "192.168.8.95:80", "localhost:18766"} {
		if validLabListenAddress(address) {
			t.Errorf("accepted out-of-scope listener %q", address)
		}
	}
}

func TestRevocationClosesOnlyMatchingConnection(t *testing.T) {
	dir := t.TempDir()
	registry := filepath.Join(dir, "devices.json")
	a, ar := net.Pipe()
	defer a.Close()
	defer ar.Close()
	b, br := net.Pipe()
	defer b.Close()
	defer br.Close()
	active := &connections{entries: map[net.Conn]string{a: "device-a", b: "device-b"}}
	if err := os.WriteFile(filepath.Join(dir, "revoked-devices.json"), []byte(`["device-a"]`), 0600); err != nil {
		t.Fatal(err)
	}
	if n := active.cutoff(registry); n != 1 {
		t.Fatalf("cutoff %d", n)
	}
	_ = ar.SetReadDeadline(time.Now().Add(time.Second))
	var buf [1]byte
	if _, err := ar.Read(buf[:]); err == nil {
		t.Fatal("revoked connection survived")
	}
	_ = br.SetReadDeadline(time.Now().Add(time.Second))
	go func() { _, _ = b.Write([]byte{1}) }()
	if _, err := br.Read(buf[:]); err != nil {
		t.Fatal("peer connection cut", err)
	}
	if err := os.WriteFile(filepath.Join(dir, "revoked-devices.json"), []byte(`invalid`), 0600); err != nil {
		t.Fatal(err)
	}
	if !isRevoked(registry, "device-b") {
		t.Fatal("malformed control failed open")
	}
}

func TestRevocationMalformedControlsFailClosed(t *testing.T) {
	dir := t.TempDir()
	registry := filepath.Join(dir, "devices.json")
	control := filepath.Join(dir, "revoked-devices.json")
	if isRevoked(registry, "peer") {
		t.Fatal("absent control blocks peer")
	}
	for _, body := range []string{` null `, `{}`, `[""]`, `[1]`} {
		if err := os.WriteFile(control, []byte(body), 0600); err != nil {
			t.Fatal(err)
		}
		if !isRevoked(registry, "peer") {
			t.Fatalf("failed open for %q", body)
		}
	}
	if err := os.WriteFile(control, []byte(`[]`), 0600); err != nil {
		t.Fatal(err)
	}
	if isRevoked(registry, "peer") {
		t.Fatal("empty list blocked peer")
	}
}

func TestGenerationChangeClosesCachedCookieConnection(t *testing.T) {
	dir := t.TempDir()
	registry := filepath.Join(dir, "devices.json")
	content := `{"hardware":{"deviceId":"device-a","sessionId":"new-cookie","cookieName":"Cookie","sessionExpiresAt":9999999999,"credentialState":"active"}}`
	if err := os.WriteFile(registry, []byte(content), 0600); err != nil {
		t.Fatal(err)
	}
	a, r := net.Pipe()
	defer a.Close()
	defer r.Close()
	active := &connections{entries: map[net.Conn]string{a: "device-a"}, cookies: map[net.Conn]string{a: "Cookie=old-cookie"}}
	if active.cutoff(registry) != 1 {
		t.Fatal("old-generation socket survived new session")
	}
}

func TestExpiredSessionVersusRetiredGrant(t *testing.T) {
	dir := t.TempDir()
	registry := filepath.Join(dir, "devices.json")
	request := httptest.NewRequest("GET", "http://localhost/neocloud/_blipsync", nil)
	request.AddCookie(&http.Cookie{Name: "Cookie", Value: "expired-cookie"})
	for _, tc := range []struct {
		state  string
		cookie string
		status int
	}{
		{"active", "expired-cookie", 401}, {"retired", "expired-cookie", 403}, {"active", "different-cookie", 403},
	} {
		body := fmt.Sprintf(`{"hardware":{"deviceId":"device-a","sessionId":%q,"cookieName":"Cookie","sessionExpiresAt":1,"credentialState":%q}}`, tc.cookie, tc.state)
		if err := os.WriteFile(registry, []byte(body), 0600); err != nil {
			t.Fatal(err)
		}
		if got := sessionRejectionStatus(request, registry); got != tc.status {
			t.Fatalf("status %d want %d", got, tc.status)
		}
	}
	for _, body := range []string{`invalid`, `null`, `{}`} {
		if err := os.WriteFile(registry, []byte(body), 0600); err != nil {
			t.Fatal(err)
		}
		if sessionRejectionStatus(request, registry) != 403 {
			t.Fatal("malformed grant accepted")
		}
	}
	body := `{"hardware":{"deviceId":"device-a","sessionId":"expired-cookie","cookieName":"Cookie","sessionExpiresAt":1}}`
	if err := os.WriteFile(registry, []byte(body), 0600); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(dir, "revoked-devices.json"), []byte(`["device-a"]`), 0600); err != nil {
		t.Fatal(err)
	}
	if sessionRejectionStatus(request, registry) != 403 {
		t.Fatal("revoked grant treated as renewable")
	}
}
