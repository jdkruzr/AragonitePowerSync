package main

import (
	"crypto/md5"
	"fmt"
	"net/http/httptest"
	"testing"
)

func TestAnonymousAuthentication(t *testing.T) {
	username := "001122aabbcc"
	digest := md5.Sum([]byte(username))
	password := fmt.Sprintf("%x", digest)
	for _, test := range []struct {
		name, user, password string
		valid                bool
	}{
		{"vendor-derived anonymous", username, password, true},
		{"wrong password", username, "wrong", false},
		{"account bearer is not anonymous", "account-id", password, false},
		{"oversized name", username + username, password, false},
	} {
		t.Run(test.name, func(t *testing.T) {
			r := httptest.NewRequest("GET", "/neocloud/_blipsync", nil)
			r.SetBasicAuth(test.user, test.password)
			if validAnonymousAuth(r) != test.valid {
				t.Fatal("unexpected authentication result")
			}
		})
	}
	r := httptest.NewRequest("GET", "/neocloud/_blipsync", nil)
	r.Header.Set("Authorization", "Bearer synthetic-token")
	if validAnonymousAuth(r) {
		t.Fatal("bearer authentication must be rejected")
	}
}
