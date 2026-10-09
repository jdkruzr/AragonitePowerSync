package main

import (
	"crypto/sha256"
	"fmt"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
)

func TestPublicRoutesAndCnameNormalization(t *testing.T) {
	dir := t.TempDir()
	registry := filepath.Join(dir, "devices.json")
	sum := sha256.Sum256([]byte("known-cpu"))
	raw := fmt.Sprintf(`{"%x":{"deviceId":"device-a","model":"Palma2_Pro_C"}}`, sum)
	if err := os.WriteFile(registry, []byte(raw), 0600); err != nil {
		t.Fatal(err)
	}
	for _, path := range []string{"/neocloud/_config", "/neocloud/_all_docs", "/api/1/users/me"} {
		r := httptest.NewRequest("GET", path, nil)
		if routeKind(r, registry) != "denied" {
			t.Fatal("uncredentialed route admitted", path)
		}
	}
	r := httptest.NewRequest("POST", "/api/v2/users/signin", nil)
	r.Header.Set("deviceUniqueId", "known-cpu")
	if routeKind(r, registry) != "account" {
		t.Fatal("known native identity rejected")
	}
	r = httptest.NewRequest("GET", "/neocloud/_config", nil)
	r.Header.Set("Authorization", "OSS fake:key")
	if routeKind(r, registry) != "denied" {
		t.Fatal("OSS header exposed administration")
	}
	r = httptest.NewRequest("GET", "/powersync-lab/note/fixture/resource/data/file.html", nil)
	r.Header.Set("Authorization", "OSS fake:key")
	if routeKind(r, registry) != "asset" || r.URL.Path != "/powersync-lab/powersync-lab/note/fixture/resource/data/file.html" {
		t.Fatal("CNAME canonical path mismatch")
	}
	r = httptest.NewRequest("GET", "/?prefix=opaque", nil)
	r.Header.Set("Authorization", "OSS fake:key")
	if routeKind(r, registry) != "asset" || r.URL.Path != "/powersync-lab/" {
		t.Fatal("CNAME listing mismatch")
	}
	r = httptest.NewRequest("GET", "/neocloud/_blipsync?admin=true", nil)
	if routeKind(r, registry) != "denied" {
		t.Fatal("replication query admitted")
	}
}

func TestLabTargetsRejectUnboundedOrPlaintextLAN(t *testing.T) {
	for _, tc := range []struct {
		listen, backend string
		tls, want       bool
	}{
		{"127.0.0.1:18770", "127.0.0.1", false, true},
		{"192.168.8.95:19443", "192.168.8.95", true, true},
		{"192.168.8.95:19443", "192.168.8.95", false, false},
		{"0.0.0.0:19443", "127.0.0.1", true, false},
		{"192.168.8.95:19443", "example.com", true, false},
	} {
		if got := validLabTargets(tc.listen, tc.backend, tc.tls); got != tc.want {
			t.Fatalf("targets %q/%q TLS=%v: %v", tc.listen, tc.backend, tc.tls, got)
		}
	}
}
