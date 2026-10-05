// A loopback-only BLIP handshake probe. It never supplies or persists revisions.
package main

import (
	"context"
	"crypto/md5"
	"crypto/subtle"
	"encoding/hex"
	"encoding/json"
	"flag"
	"fmt"
	"net/http"
	"os"
	"regexp"
	"sync"
	"time"

	blip "github.com/couchbase/go-blip"
)

var anonymousName = regexp.MustCompile(`^[0-9a-fA-F]{12}$`)

func validAnonymousAuth(r *http.Request) bool {
	username, password, ok := r.BasicAuth()
	if !ok || !anonymousName.MatchString(username) {
		return false
	}
	digest := md5.Sum([]byte(username)) // Matches vendor anonymous gateway algorithm.
	return subtle.ConstantTimeCompare([]byte(password), []byte(hex.EncodeToString(digest[:]))) == 1
}

func main() {
	port := flag.Int("port", 18766, "loopback listener port")
	output := flag.String("output", "", "JSONL event output; no credentials or request bodies")
	flag.Parse()
	if *port < 1024 || *port > 65535 || *output == "" {
		fmt.Fprintln(os.Stderr, "valid --port and --output required")
		os.Exit(2)
	}
	file, err := os.OpenFile(*output, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0600)
	if err != nil {
		panic(err)
	}
	defer file.Close()
	var lock sync.Mutex
	record := func(value map[string]any) {
		lock.Lock()
		defer lock.Unlock()
		value["at"] = time.Now().UTC().Format(time.RFC3339Nano)
		_ = json.NewEncoder(file).Encode(value)
	}
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/neocloud/_blipsync" {
			http.NotFound(w, r)
			return
		}
		if !validAnonymousAuth(r) {
			record(map[string]any{"event": "authentication-rejected"})
			w.Header().Set("WWW-Authenticate", `Basic realm="PowerSync anonymous lab"`)
			http.Error(w, "anonymous lab credentials required", 401)
			return
		}
		ctx, cancel := context.WithTimeout(r.Context(), 15*time.Second)
		defer cancel()
		protocol, err := blip.NewContext(blip.ContextOptions{ProtocolIds: []string{"CBMobile_3", "CBMobile_2"}, CancelCtx: ctx})
		if err != nil {
			http.Error(w, "protocol setup failed", 500)
			return
		}
		// Library errors can include message properties. Keep logging metadata only.
		protocol.Logger = func(blip.LogEventType, string, ...interface{}) {}
		protocol.DefaultHandler = func(request *blip.Message) {
			profile := request.Profile()
			switch profile {
			case "getCheckpoint", "setCheckpoint", "getCollections", "subChanges", "changes", "proposeChanges", "rev", "norev", "getAttachment", "proveAttachment":
			default:
				profile = "other"
			}
			record(map[string]any{"event": "blip-request", "profile": profile})
			if response := request.Response(); response != nil {
				response.SetError(blip.BLIPErrorDomain, 501, "routing probe; no replication implemented")
			}
		}
		socket := protocol.WebSocketServer()
		socket.PostHandshakeCallback = func(err error) {
			record(map[string]any{"event": "websocket-handshake", "accepted": err == nil, "anonymousCredentialsValidated": true, "protocol": protocol.ActiveSubprotocol()})
		}
		socket.ServeHTTP(w, r)
	})
	server := http.Server{Addr: fmt.Sprintf("127.0.0.1:%d", *port), Handler: handler, ReadHeaderTimeout: 5 * time.Second}
	fmt.Printf("PowerSync authenticated anonymous BLIP probe on %s\n", server.Addr)
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
