from app import app


if __name__ == "__main__":
    import os

    from waitress import serve

    host = "0.0.0.0"
    port = int(os.environ.get("PORT", "5001"))
    print(f"AgroScan model service listening on http://{host}:{port}", flush=True)
    print("Press Ctrl+C to stop.", flush=True)
    serve(app, host=host, port=port)
