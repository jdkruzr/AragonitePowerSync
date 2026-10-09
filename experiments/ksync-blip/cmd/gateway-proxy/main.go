// Transparent, session-bound lab proxy. Sync Gateway owns every BLIP message.
package main

import (
	"bufio"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"os/signal"
	"path/filepath"
	"strconv"
	"sync"
	"time"
)

type device struct {
	DeviceID   string  `json:"deviceId"`
	SessionID  string  `json:"sessionId"`
	CookieName string  `json:"cookieName"`
	Expires    float64 `json:"sessionExpiresAt"`
	State      string  `json:"credentialState"`
}

func validLabListenAddress(address string) bool {
	host, port, err := net.SplitHostPort(address)
	if err != nil {
		return false
	}
	ip := net.ParseIP(host)
	number, err := strconv.Atoi(port)
	return err == nil && number >= 1024 && number <= 65535 && ip != nil && !ip.IsUnspecified() && !ip.IsMulticast() && (ip.IsLoopback() || ip.IsPrivate())
}

func identify(r *http.Request, registry string) (string, bool) {
	b, err := os.ReadFile(registry)
	if err != nil {
		return "", false
	}
	var devices map[string]device
	if json.Unmarshal(b, &devices) != nil {
		return "", false
	}
	for _, d := range devices {
		if (d.State != "" && d.State != "active") || d.SessionID == "" || d.Expires <= float64(time.Now().Unix()) {
			continue
		}
		cookie, err := r.Cookie(d.CookieName)
		if err == nil && cookie.Value == d.SessionID {
			return d.DeviceID, true
		}
	}
	return "", false
}

// Expiry permits renewal; retired or unknown grants remain forbidden.
func sessionRejectionStatus(r *http.Request, registry string) int {
	b, err := os.ReadFile(registry)
	if err != nil {
		return http.StatusForbidden
	}
	var devices map[string]device
	if json.Unmarshal(b, &devices) != nil {
		return http.StatusForbidden
	}
	for _, d := range devices {
		if (d.State != "" && d.State != "active") || d.SessionID == "" || isRevoked(registry, d.DeviceID) {
			continue
		}
		cookie, err := r.Cookie(d.CookieName)
		if err == nil && cookie.Value == d.SessionID && d.Expires <= float64(time.Now().Unix()) {
			return http.StatusUnauthorized
		}
	}
	return http.StatusForbidden
}

func isRevoked(registry, id string) bool {
	b, err := os.ReadFile(filepath.Join(filepath.Dir(registry), "revoked-devices.json"))
	if os.IsNotExist(err) {
		return false
	}
	if err != nil || len(b) > 65536 {
		return true
	}
	var ids []string
	if json.Unmarshal(b, &ids) != nil || ids == nil {
		return true
	}
	for _, item := range ids {
		if item == "" || len(item) > 64 {
			return true
		}
		if item == id {
			return true
		}
	}
	return false
}

type connections struct {
	mu      sync.Mutex
	entries map[net.Conn]string
	cookies map[net.Conn]string
}

func (c *connections) cutoff(registry string) int {
	c.mu.Lock()
	var doomed []net.Conn
	for conn, id := range c.entries {
		request := &http.Request{Header: make(http.Header)}
		request.Header.Set("Cookie", c.cookies[conn])
		current, valid := identify(request, registry)
		if isRevoked(registry, id) || (c.cookies[conn] != "" && (!valid || current != id)) {
			doomed = append(doomed, conn)
			delete(c.entries, conn)
			delete(c.cookies, conn)
		}
	}
	c.mu.Unlock()
	for _, conn := range doomed {
		_ = conn.Close()
	}
	return len(doomed)
}

type trackedWriter struct {
	http.ResponseWriter
	connections *connections
	id          string
	cookie      string
	conn        net.Conn
}

func (w *trackedWriter) Hijack() (net.Conn, *bufio.ReadWriter, error) {
	conn, rw, err := http.NewResponseController(w.ResponseWriter).Hijack()
	if err == nil {
		w.conn = conn
		w.connections.mu.Lock()
		w.connections.entries[conn] = w.id
		if w.connections.cookies == nil {
			w.connections.cookies = make(map[net.Conn]string)
		}
		w.connections.cookies[conn] = w.cookie
		w.connections.mu.Unlock()
	}
	return conn, rw, err
}
func (w *trackedWriter) Flush()                      { _ = http.NewResponseController(w.ResponseWriter).Flush() }
func (w *trackedWriter) Unwrap() http.ResponseWriter { return w.ResponseWriter }
func (w *trackedWriter) release() {
	if w.conn != nil {
		w.connections.mu.Lock()
		delete(w.connections.entries, w.conn)
		delete(w.connections.cookies, w.conn)
		w.connections.mu.Unlock()
	}
}

func main() {
	registry := flag.String("registry", "", "private shim registry file")
	output := flag.String("events", "", "connection events, no secrets or payloads")
	listen := flag.String("listen", "127.0.0.1:18766", "concrete private-interface lab listener")
	flag.Parse()
	if !validLabListenAddress(*listen) {
		log.Fatal("concrete private-interface address and unprivileged port required")
	}
	if *registry == "" || *output == "" {
		log.Fatal("registry and events are required")
	}
	f, err := os.OpenFile(*output, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0600)
	if err != nil {
		log.Fatal("cannot open event output")
	}
	defer f.Close()
	var mu sync.Mutex
	emit := func(event map[string]any) {
		event["at"] = time.Now().UTC().Format(time.RFC3339Nano)
		mu.Lock()
		defer mu.Unlock()
		_ = json.NewEncoder(f).Encode(event)
	}
	active := &connections{entries: make(map[net.Conn]string)}
	target, _ := url.Parse("http://127.0.0.1:18767")
	proxy := httputil.NewSingleHostReverseProxy(target)
	proxy.ErrorLog = log.New(os.Stderr, "gateway transport error: ", 0)
	proxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		emit(map[string]any{"event": "upstream_error"})
		http.Error(w, "Gateway unavailable", 502)
	}
	proxy.ModifyResponse = func(response *http.Response) error {
		id, _ := identify(response.Request, *registry)
		emit(map[string]any{"event": "response", "deviceId": id, "status": response.StatusCode,
			"protocol": response.Header.Get("Sec-WebSocket-Protocol")})
		return nil
	}
	server := &http.Server{Addr: *listen, ReadHeaderTimeout: 10 * time.Second,
		Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			id, ok := identify(r, *registry)
			if !ok || isRevoked(*registry, id) || r.Method != "GET" || r.URL.Path != "/neocloud/_blipsync" || r.URL.RawQuery != "" || r.Header.Get("Authorization") != "" {
				status := http.StatusForbidden
				if !ok && r.Method == "GET" && r.URL.Path == "/neocloud/_blipsync" && r.URL.RawQuery == "" && r.Header.Get("Authorization") == "" {
					status = sessionRejectionStatus(r, *registry)
				}
				emit(map[string]any{"event": "refused", "identified": ok, "status": status})
				http.Error(w, "Lab session required", status)
				return
			}
			peer, _, _ := net.SplitHostPort(r.RemoteAddr)
			emit(map[string]any{"event": "connect", "deviceId": id, "peerIP": peer})
			tracked := &trackedWriter{ResponseWriter: w, connections: active, id: id, cookie: r.Header.Get("Cookie")}
			defer tracked.release()
			proxy.ServeHTTP(tracked, r)
		})}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt)
	defer stop()
	go func() {
		ticker := time.NewTicker(250 * time.Millisecond)
		defer ticker.Stop()
		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				if n := active.cutoff(*registry); n > 0 {
					emit(map[string]any{"event": "revocation_cutoff", "connections": n})
				}
			}
		}
	}()
	go func() {
		<-ctx.Done()
		shutdown, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = server.Shutdown(shutdown)
	}()
	fmt.Println("Session-bound Gateway proxy listening on selected private interface; BLIP forwarded unchanged.")
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatal("listener failed")
	}
}
