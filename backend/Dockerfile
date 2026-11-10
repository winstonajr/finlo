FROM golang:1.24-alpine AS builder

WORKDIR /app

RUN apk add --no-cache ca-certificates git

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o /app/finlo-server ./cmd/server

FROM alpine:3.21

WORKDIR /app

RUN apk add --no-cache ca-certificates tzdata \
    && addgroup -S finlo && adduser -S finlo -G finlo

COPY --from=builder /app/finlo-server /app/finlo-server
COPY --from=builder /app/migrations /app/migrations

USER finlo

EXPOSE 8080

ENTRYPOINT ["/app/finlo-server"]
