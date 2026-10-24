"""
Standalone Wi-Fi APK Sharing Server
Enables direct Wi-Fi download of the Android APK onto any mobile phone
connected to the same local network.
"""

import os
import sys
import socket
import base64
import io
from http.server import HTTPServer, BaseHTTPRequestHandler
from pathlib import Path

APK_PRIMARY = Path(__file__).parent / "downloads" / "SharyarTradingSystem.apk"
APK_FALLBACK = Path(__file__).parent / "android_app" / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
APK_PATH = APK_PRIMARY if APK_PRIMARY.exists() else APK_FALLBACK
ASSETS_DIR = Path(__file__).parent / "assets"
DEFAULT_PORT = 8080

def get_lan_ip() -> str:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

def generate_qr_base64(url: str) -> str:
    try:
        import qrcode
        qr = qrcode.QRCode(box_size=8, border=2)
        qr.add_data(url)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white")
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        return base64.b64encode(buf.getvalue()).decode("utf-8")
    except Exception as e:
        return ""

def generate_qr_ascii(url: str) -> str:
    try:
        import qrcode
        qr = qrcode.QRCode(box_size=1, border=2)
        qr.add_data(url)
        qr.make(fit=True)
        buf = io.StringIO()
        qr.print_ascii(out=buf, invert=True)
        return buf.getvalue()
    except Exception:
        return ""

class ApkDownloadHandler(BaseHTTPRequestHandler):
    lan_ip = "127.0.0.1"
    port = DEFAULT_PORT

    def log_message(self, format, *args):
        # Clean logging
        sys.stdout.write(f"[*] [Wi-Fi Server] {self.address_string()} - {format%args}\n")
        sys.stdout.flush()

    def do_HEAD(self):
        url_path = self.path.split("?")[0]
        if url_path in ("/download", "/app-debug.apk", "/binance-terminal.apk"):
            if not APK_PATH.exists():
                self.send_error(404, "APK not found")
                return
            file_size = APK_PATH.stat().st_size
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="BinanceTerminal-debug.apk"')
            self.send_header("Content-Length", str(file_size))
            self.send_header("Accept-Ranges", "bytes")
            self.end_headers()
        else:
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()

    def do_GET(self):
        url_path = self.path.split("?")[0]

        if url_path in ("/download", "/app-debug.apk", "/binance-terminal.apk"):
            self.serve_apk()
        elif url_path == "/qr.png":
            self.serve_qr_png()
        else:
            self.serve_landing_page()

    def serve_apk(self):
        if not APK_PATH.exists():
            self.send_error(404, "APK file not found. Please build with gradlew assembleDebug.")
            return

        file_size = APK_PATH.stat().st_size
        self.send_response(200)
        self.send_header("Content-Type", "application/vnd.android.package-archive")
        self.send_header("Content-Disposition", 'attachment; filename="BinanceTerminal-debug.apk"')
        self.send_header("Content-Length", str(file_size))
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()

        with open(APK_PATH, "rb") as f:
            while chunk := f.read(65536):
                self.wfile.write(chunk)

    def serve_qr_png(self):
        qr_file = ASSETS_DIR / "apk_wifi_qr.png"
        if qr_file.exists():
            data = qr_file.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "image/png")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        else:
            self.send_error(404, "QR image not found")

    def serve_landing_page(self):
        download_url = f"http://{self.lan_ip}:{self.port}/download"
        qr_b64 = generate_qr_base64(download_url)
        apk_size_mb = f"{APK_PATH.stat().st_size / (1024 * 1024):.1f} MB" if APK_PATH.exists() else "19.5 MB"

        html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sharyar Trading System - Wi-Fi APK Download</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{
            background: #0b0e14;
            color: #e2e8f0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }}
        .card {{
            background: #141a24;
            border: 1px solid #2d3748;
            border-radius: 16px;
            max-width: 480px;
            width: 100%;
            padding: 32px 24px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
            text-align: center;
        }}
        .badge {{
            display: inline-block;
            background: rgba(240, 185, 11, 0.15);
            color: #f0b90b;
            border: 1px solid #f0b90b;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 16px;
        }}
        h1 {{
            font-size: 24px;
            color: #ffffff;
            margin-bottom: 8px;
            font-weight: 800;
        }}
        p.subtitle {{
            color: #94a3b8;
            font-size: 14px;
            margin-bottom: 24px;
            line-height: 1.5;
        }}
        .qr-box {{
            background: #ffffff;
            padding: 16px;
            border-radius: 12px;
            display: inline-block;
            margin-bottom: 24px;
            box-shadow: 0 8px 16px rgba(0, 0, 0, 0.3);
        }}
        .qr-box img {{
            display: block;
            width: 180px;
            height: 180px;
        }}
        .btn-download {{
            display: block;
            background: linear-gradient(135deg, #f0b90b 0%, #f59e0b 100%);
            color: #0b0e14;
            text-decoration: none;
            padding: 16px 24px;
            border-radius: 12px;
            font-size: 17px;
            font-weight: 800;
            letter-spacing: 0.5px;
            box-shadow: 0 8px 24px rgba(240, 185, 11, 0.35);
            transition: all 0.2s ease;
            margin-bottom: 24px;
        }}
        .btn-download:active {{
            transform: scale(0.98);
            box-shadow: 0 4px 12px rgba(240, 185, 11, 0.2);
        }}
        .meta {{
            background: #0b0e14;
            border: 1px solid #1e293b;
            border-radius: 10px;
            padding: 12px 16px;
            margin-bottom: 24px;
            font-size: 13px;
            color: #94a3b8;
            display: flex;
            justify-content: space-around;
        }}
        .meta strong {{ color: #00f2fe; }}
        .steps {{
            text-align: left;
            background: #0f172a;
            border-radius: 10px;
            padding: 16px;
            font-size: 13px;
            color: #cbd5e1;
            line-height: 1.6;
        }}
        .steps h3 {{
            color: #ffffff;
            font-size: 14px;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 6px;
        }}
        .steps ol {{
            padding-left: 20px;
        }}
        .steps li {{
            margin-bottom: 6px;
        }}
    </style>
</head>
<body>
    <div class="card">
        <span class="badge">Standalone Mobile App</span>
        <h1>⚡ Sharyar Trading System</h1>
        <p class="subtitle">Direct Wi-Fi download to your Android phone.<br>Autonomous AI Multi-Strategy & Mega-Move Mobile App.</p>

        <div class="qr-box">
            <img src="data:image/png;base64,{qr_b64}" alt="Download QR Code">
        </div>

        <a href="/download" class="btn-download">
            ⬇️ Download APK ({apk_size_mb})
        </a>

        <div class="meta">
            <div>Format: <strong>APK</strong></div>
            <div>Size: <strong>{apk_size_mb}</strong></div>
            <div>Build: <strong>Debug</strong></div>
        </div>

        <div class="steps">
            <h3>📱 Quick Install Steps:</h3>
            <ol>
                <li>Tap <strong>Download APK</strong> above.</li>
                <li>If prompted with <em>"File might be harmful"</em>, tap <strong>Download anyway</strong>.</li>
                <li>When download finishes, tap <strong>Open</strong>.</li>
                <li>If prompted, tap <strong>Settings</strong> &gt; enable <strong>Allow from this source</strong>.</li>
                <li>Tap <strong>Install</strong> and enjoy standalone Binance scanning!</li>
            </ol>
        </div>
    </div>
</body>
</html>
"""
        data = html.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

def run_server(port=DEFAULT_PORT):
    lan_ip = get_lan_ip()
    ApkDownloadHandler.lan_ip = lan_ip
    ApkDownloadHandler.port = port

    # Ensure QR image exists
    download_url = f"http://{lan_ip}:{port}/download"
    landing_url = f"http://{lan_ip}:{port}/"
    
    try:
        import qrcode
        qr = qrcode.QRCode(box_size=8, border=2)
        qr.add_data(landing_url)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white")
        ASSETS_DIR.mkdir(parents=True, exist_ok=True)
        img.save(str(ASSETS_DIR / "apk_wifi_qr.png"))
    except Exception:
        pass

    ascii_qr = generate_qr_ascii(landing_url)

    server = HTTPServer(("0.0.0.0", port), ApkDownloadHandler)
    print("=" * 65)
    print("   ⚡ SHARYAR TRADING SYSTEM - WI-FI APK DOWNLOAD SERVER")
    print("=" * 65)
    print(f"\n[*] Server running on all interfaces (Port {port})")
    print(f"\n📱 OPEN THIS URL ON YOUR ANDROID PHONE'S BROWSER:")
    print(f"\n   >>>  {landing_url}  <<<")
    print(f"\n⚡ DIRECT APK DOWNLOAD LINK:")
    print(f"   >>>  {download_url}  <<<\n")
    if ascii_qr:
        print("📱 OR SCAN THIS QR CODE WITH YOUR PHONE'S CAMERA:\n")
        print(ascii_qr)
    print("=" * 65)
    print("[*] Waiting for incoming download requests from your phone...")
    print("[*] Press Ctrl+C to stop the server when finished.")
    print("=" * 65 + "\n")
    sys.stdout.flush()

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[*] Server stopped.")
        server.server_close()

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_PORT
    run_server(port)
