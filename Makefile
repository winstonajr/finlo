.PHONY: all build-backend test-backend test-backend-race docker-up docker-down clean

all: build-backend test-backend

build-backend:
	cd backend && go build -v -o bin/server ./cmd/server

test-backend:
	cd backend && go test -v ./...

test-backend-race:
	cd backend && go test -v -race ./...

docker-up:
	docker compose up -d --build

docker-down:
	docker compose down -v

clean:
	rm -rf backend/bin
