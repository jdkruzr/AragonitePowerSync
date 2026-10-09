package main

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"strings"
	"time"
)

type device struct {
	DeviceID string `json:"deviceId"`
	Model    string `json:"model"`
}

func admitted(r *http.Request, registry string) bool {
	raw, err := os.ReadFile(registry)
	if err != nil {
		return false
	}
	var rows map[string]device
	if json.Unmarshal(raw, &rows) != nil {
		return false
	}
	id := r.Header.Get("deviceUniqueId")
	if id == "" {
		id = r.Header.Get("mac")
	}
	sum := sha256.Sum256([]byte(strings.ToLower(strings.TrimSpace(id))))
	d, ok := rows[hex.EncodeToString(sum[:])]
	return ok && (d.Model == "Palma2_Pro_C" || d.Model == "Go103_2Lumi")
}
func routeKind(r *http.Request, registry string) string {
	kind := "denied"
	switch {
	case r.URL.Path == "/neocloud/_blipsync" && r.Method == "GET" && r.URL.RawQuery == "":
		kind = "replication"
	case strings.HasPrefix(r.Header.Get("Authorization"), "OSS ") && (r.URL.Path == "/" || strings.HasPrefix(r.URL.Path, "/powersync-lab/")):
		kind = "asset"
		if r.URL.Path == "/" {
			r.URL.Path = "/powersync-lab/"
		} else if strings.HasPrefix(r.URL.Path, "/powersync-lab/note/") || strings.HasPrefix(r.URL.Path, "/powersync-lab/reader/") {
			r.URL.Path = "/powersync-lab" + r.URL.Path
		}
		r.URL.RawPath = ""
	case strings.HasPrefix(r.URL.Path, "/api/") && admitted(r, registry):
		kind = "account"
	case r.URL.Path == "/health" && r.Method == "GET":
		kind = "health"
	}
	return kind
}
func validLabTargets(listen, backend string, tls bool) bool {
	if backend != "127.0.0.1" && backend != "192.168.8.95" {
		return false
	}
	return listen == "127.0.0.1:18770" || (listen == "192.168.8.95:19443" && tls)
}
func main() {
	backendHost := flag.String("backend-host", "127.0.0.1", "fixed lab backend interface")
	listen := flag.String("listen", "127.0.0.1:18770", "lab listener")
	cert := flag.String("tls-cert", "", "lab TLS certificate")
	key := flag.String("tls-key", "", "lab TLS key")
	registry := flag.String("registry", "", "private device registry")
	events := flag.String("events", "", "private request receipts")
	flag.Parse()
	if (*cert == "") != (*key == "") {
		log.Fatal("TLS certificate and key must be supplied together")
	}
	if !validLabTargets(*listen, *backendHost, *cert != "") {
		log.Fatal("fixed lab listener/backend and TLS on LAN required")
	}
	if *registry == "" || *events == "" {
		log.Fatal("registry/events required")
	}
	f, err := os.OpenFile(*events, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0600)
	if err != nil {
		log.Fatal("journal unavailable")
	}
	defer f.Close()
	accountURL, _ := url.Parse("http://" + *backendHost + ":18765")
	syncURL, _ := url.Parse("http://" + *backendHost + ":18766")
	account := httputil.NewSingleHostReverseProxy(accountURL)
	replication := httputil.NewSingleHostReverseProxy(syncURL)
	// Requests contain no logged headers, query values, credentials, or payloads.
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		kind := routeKind(r, *registry)
		raw, _ := json.Marshal(map[string]any{"at": time.Now().UnixMilli(), "kind": kind, "method": r.Method})
		_, _ = f.Write(append(raw, '\n'))
		w.Header().Set("Cache-Control", "no-store")
		if kind == "denied" {
			http.Error(w, "Lab route denied", 403)
			return
		}
		if kind == "replication" {
			replication.ServeHTTP(w, r)
		} else {
			account.ServeHTTP(w, r)
		}
	})
	server := &http.Server{Addr: *listen, Handler: handler, ReadHeaderTimeout: 10 * time.Second}
	if *cert != "" {
		log.Fatal(server.ListenAndServeTLS(*cert, *key))
	}
	log.Fatal(server.ListenAndServe())
}
