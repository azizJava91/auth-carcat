"""Flutter-facing changelog PDF: auth API rename (old → new)."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "auth_api_changes_flutter.pdf"
FONT = Path(r"C:\Windows\Fonts\arial.ttf")
FONT_BOLD = Path(r"C:\Windows\Fonts\arialbd.ttf")
FONT_ITALIC = Path(r"C:\Windows\Fonts\ariali.ttf")


class Pdf(FPDF):
    def __init__(self):
        super().__init__()
        self.add_font("T", "", str(FONT))
        self.add_font("T", "B", str(FONT_BOLD if FONT_BOLD.exists() else FONT))
        self.add_font("T", "I", str(FONT_ITALIC if FONT_ITALIC.exists() else FONT))
        self._f = "T"

    def header(self):
        self.set_font(self._f, "", 8)
        self.set_text_color(110, 110, 110)
        self.cell(0, 8, "carland_auth - API changes (Flutter)", align="R")
        self.ln(3)

    def footer(self):
        self.set_y(-14)
        self.set_font(self._f, "", 8)
        self.set_text_color(110, 110, 110)
        self.cell(0, 10, f"Page {self.page_no()}", align="C")

    def h1(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 15)
        self.set_text_color(15, 15, 15)
        self.multi_cell(self.epw, 8, t)
        self.ln(2)

    def h2(self, t):
        self.ln(2)
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 11)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 6, t)
        self.ln(0.5)

    def row(self, old, new):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 9)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 4.5, f"OLD: {old}")
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 9)
        self.multi_cell(self.epw, 4.5, f"NEW: {new}")
        self.ln(1.5)

    def code(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 8)
        self.set_fill_color(242, 242, 242)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 4, t, fill=True)
        self.ln(1)


def build():
    pdf = Pdf()
    pdf.set_margins(14, 14, 14)
    pdf.set_auto_page_break(auto=True, margin=16)
    pdf.add_page()

    pdf.h1("carland_auth API changes — Flutter")
    pdf.set_font(pdf._f, "", 10)
    pdf.multi_cell(pdf.epw, 5, "Old → New mapping. Base path unchanged: /api/v1/users and /api/v1/otp")
    pdf.ln(1)

    pdf.h2("1. Endpoints")
    pdf.row(
        "POST /api/v1/users/register",
        "POST /api/v1/users/authentication",
    )
    pdf.row(
        "PUT /api/v1/users/setPassword",
        "PUT /api/v1/users/set/pin",
    )
    pdf.row(
        "PUT /api/v1/users/updatePassword",
        "PUT /api/v1/users/update/pin",
    )
    pdf.row(
        "POST /api/v1/users/login  (path same)",
        "POST /api/v1/users/login  (body field changed)",
    )
    pdf.row(
        "POST /api/v1/otp/*  (path same)",
        "POST /api/v1/otp/*  (token name in response chain changed)",
    )

    pdf.h2("2. Request body fields")
    pdf.row(
        '{ "phoneNumber", "password", "name", "surname" }',
        '{ "phoneNumber", "pin", "name", "surname" }',
    )
    pdf.row(
        "InviteRequest.password",
        "InviteRequest.pin",
    )
    pdf.row(
        "pin format: free text password",
        "pin: exactly 4 digits, not all same (e.g. 5555 invalid)",
    )

    pdf.h2("3. Response fields")
    pdf.row(
        "RegisterResponse.registerToken",
        "AuthenticationResponse.authenticationToken",
    )
    pdf.row(
        "UserResponse (no password field) — unchanged shape",
        "UserResponse — unchanged; login uses pin in request only",
    )

    pdf.h2("4. Auth header usage (flow)")
    pdf.row(
        "Authorization: Bearer <registerToken>",
        "Authorization: Bearer <authenticationToken>",
    )
    pdf.code(
        "Used on:\n"
        "  POST /api/v1/otp/createAndSend\n"
        "  POST /api/v1/otp/verify\n"
        "  PUT  /api/v1/users/set/pin"
    )

    pdf.h2("5. Example — authenticate")
    pdf.code(
        "OLD:\n"
        "POST /api/v1/users/register?role=USER\n"
        '{ "phoneNumber": "+994...", "name": "...", "surname": "..." }\n'
        "→ { \"registerToken\": \"...\", \"message\": \"...\" }\n\n"
        "NEW:\n"
        "POST /api/v1/users/authentication?role=USER\n"
        '{ "phoneNumber": "+994...", "name": "...", "surname": "..." }\n'
        "→ { \"authenticationToken\": \"...\", \"message\": \"...\" }"
    )

    pdf.h2("6. Example — login")
    pdf.code(
        "OLD:\n"
        "POST /api/v1/users/login\n"
        '{ "phoneNumber": "+994...", "password": "..." }\n\n'
        "NEW:\n"
        "POST /api/v1/users/login\n"
        '{ "phoneNumber": "+994...", "pin": "1234" }'
    )

    pdf.h2("7. Example — set pin")
    pdf.code(
        "OLD:\n"
        "PUT /api/v1/users/setPassword\n"
        "Authorization: Bearer <registerToken>\n"
        '{ "password": "..." }\n\n'
        "NEW:\n"
        "PUT /api/v1/users/set/pin\n"
        "Authorization: Bearer <authenticationToken>\n"
        '{ "pin": "1234" }'
    )

    pdf.h2("8. Example — update pin (start OTP flow)")
    pdf.code(
        "OLD:\n"
        "PUT /api/v1/users/updatePassword\n"
        '{ "phoneNumber": "+994..." }\n'
        "→ { \"registerToken\": \"...\" }\n\n"
        "NEW:\n"
        "PUT /api/v1/users/update/pin\n"
        '{ "phoneNumber": "+994..." }\n'
        "→ { \"authenticationToken\": \"...\" }"
    )

    pdf.h2("9. Errors (PIN)")
    pdf.row(
        "N/A (old password rules)",
        "WeakPinException → HTTP 400, message az/en/ru (weak PIN)",
    )
    pdf.row(
        "Wrong password message",
        "Wrong PIN message (same exception type, new text)",
    )

    pdf.h2("10. Token TTL (config)")
    pdf.row(
        "refresh.token.expiration = 2592000 (1 month)",
        "refresh.token.expiration = 31536000 (1 year)",
    )
    pdf.row(
        "register.token.*",
        "authentication.token.* (same secret/TTL values)",
    )

    pdf.h2("11. DB (backend only — Flutter ignore)")
    pdf.row(
        "users.password",
        "users.pin  (still BCrypt hash)",
    )

    pdf.ln(2)
    pdf.set_font(pdf._f, "", 8)
    pdf.set_text_color(90, 90, 90)
    pdf.multi_cell(
        pdf.epw,
        4,
        "Regenerate: python docs/generate_auth_api_changes_flutter_pdf.py",
    )

    pdf.output(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
