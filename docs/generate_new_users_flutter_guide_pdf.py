"""Flutter guide PDF: NewUsers final paths + legacy deltas (post QA fix)."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "new_users_flutter_api_guide.pdf"
FONT = Path(r"C:\Windows\Fonts\arial.ttf")
FONT_BOLD = Path(r"C:\Windows\Fonts\arialbd.ttf")
FONT_ITALIC = Path(r"C:\Windows\Fonts\ariali.ttf")

BASE = "https://digital-innovation.agency/auth/server"


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
        self.cell(0, 8, "carland_auth — Flutter API guide (NewUsers + Legacy)", align="R")
        self.ln(3)

    def footer(self):
        self.set_y(-14)
        self.set_font(self._f, "", 8)
        self.set_text_color(110, 110, 110)
        self.cell(0, 10, f"Page {self.page_no()}", align="C")

    def h1(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 14)
        self.set_text_color(15, 15, 15)
        self.multi_cell(self.epw, 7, t)
        self.ln(2)

    def h2(self, t):
        self.ln(2)
        if self.get_y() > 250:
            self.add_page()
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 11)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 6, t)
        self.ln(0.5)

    def h3(self, t):
        self.ln(1)
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 10)
        self.set_text_color(40, 40, 40)
        self.multi_cell(self.epw, 5, t)

    def p(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 9)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 4.5, t)
        self.ln(0.5)

    def bullet(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 9)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 4.5, f"- {t}")

    def code(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 7.5)
        self.set_text_color(20, 20, 20)
        self.set_fill_color(242, 242, 242)
        self.multi_cell(self.epw, 3.8, t, fill=True)
        self.ln(1)


def build():
    pdf = Pdf()
    pdf.set_margins(14, 14, 14)
    pdf.set_auto_page_break(auto=True, margin=16)
    pdf.add_page()

    pdf.h1("carland_auth — Flutter API guide (post QA)")
    pdf.p(f"Public base: {BASE}")
    pdf.bullet("Legacy (old app): /api/v1/users and /api/v1/otp")
    pdf.bullet("NewUsers (new app): /api/v1/newUsers")
    pdf.bullet("Header: Accept-Language: az | en | ru (SMS + error text)")
    pdf.bullet("NewUsers PIN: Argon2id in pin_hash. Legacy password: BCrypt in password.")
    pdf.bullet("Weak PIN (NewUsers only): 4 digits; reject all-same (1111) and sequences (1234, 4321).")

    pdf.h2("A) NewUsers flow")
    pdf.p(
        "User is NOT created on /auth. OTP verify may create the user. setPinCode returns "
        "status PIN_SET only — then call login. authToken is in JSON body (not Bearer), TTL ~6 min. "
        "purpose lives only in the JWT (server extracts + logs it)."
    )

    pdf.h3("Screen map (response.next)")
    pdf.bullet("SEND_OTP — OTP screen → createAndSend")
    pdf.bullet("PIN_CHECK — PIN login screen → login")
    pdf.bullet("VERIFY_OTP — after createAndSend success; stay on OTP → verify")
    pdf.bullet("SET_PIN — after verify; set-PIN screen → setPinCode")

    pdf.h3("Happy paths")
    pdf.p("Register:")
    pdf.code(
        "1) POST /auth { phoneNumber }\n"
        "   → next=SEND_OTP, authToken, purpose=REGISTER\n"
        "2) POST /otp/createAndSend { authToken } → next=VERIFY_OTP\n"
        "3) POST /otp/verify { authToken, otp }\n"
        "   → next=SET_PIN, NEW authToken (replace old)\n"
        "4) PUT /setPinCode { authToken, pinCode } → { status: PIN_SET }\n"
        "5) POST /login { phoneNumber, pinCode, deviceId, platform? }\n"
        "   → accessToken + refreshToken"
    )
    pdf.p("Existing user:")
    pdf.code(
        "1) POST /auth { phoneNumber } → next=PIN_CHECK\n"
        "2) POST /login { phoneNumber, pinCode, deviceId, platform? }"
    )
    pdf.p("Forgot PIN:")
    pdf.code(
        "1) POST /auth { phoneNumber, purpose:\"RESET\" } → next=SEND_OTP\n"
        "   (no user / no PIN → treated as REGISTER, no 404)\n"
        "2) createAndSend → verify → setPinCode → login\n"
        "   RESET setPinCode revokes other devices' refresh sessions"
    )

    pdf.h2("A1) POST /api/v1/newUsers/auth")
    pdf.bullet("Auth: none | Headers: Accept-Language")
    pdf.code('{\n  "phoneNumber": "+994501234567",\n  "purpose": "RESET"   // optional\n}')
    pdf.code(
        '{\n'
        '  "authToken": "<jwt ~6 min>",\n'
        '  "next": "SEND_OTP" | "PIN_CHECK",\n'
        '  "purpose": "REGISTER" | "RESET",\n'
        '  "message": "..."\n'
        "}"
    )
    pdf.bullet("pin_hash present → PIN_CHECK; else SEND_OTP")
    pdf.bullet("429 if phone >10/min or IP >30/min on /auth")

    pdf.h2("A2) POST /api/v1/newUsers/otp/createAndSend")
    pdf.code('{\n  "authToken": "<from /auth>"\n}')
    pdf.bullet("Success next: VERIFY_OTP")
    pdf.bullet("30s cooldown; 3 sends / 15 min → phone lock ~5 min (LOGIN_LOCKED + countdown)")
    pdf.bullet("IP abuse → 24h lock, generic message, no countdown")
    pdf.bullet("401 AUTH_TOKEN_EXPIRED / INVALID_TOKEN")
    pdf.bullet("OTP never in JSON; SMS only; Accept-Language az/en/ru")

    pdf.h2("A3) POST /api/v1/newUsers/otp/verify")
    pdf.code('{\n  "authToken": "<from /auth>",\n  "otp": "123456"\n}')
    pdf.code(
        '{\n'
        '  "authToken": "<NEW jwt for setPinCode>",\n'
        '  "next": "SET_PIN",\n'
        '  "purpose": "REGISTER" | "RESET",\n'
        '  "message": "..."\n'
        "}"
    )
    pdf.bullet("401 OTP_INCORRECT; 429 OTP_VERIFY_LOCKED (~300s); 400 OTP_EXPIRED")

    pdf.h2("A4) PUT /api/v1/newUsers/setPinCode")
    pdf.bullet("No query purpose — purpose is inside authToken JWT")
    pdf.code('{\n  "authToken": "<from verify>",\n  "pinCode": "2580"\n}')
    pdf.code('{\n  "status": "PIN_SET"\n}')
    pdf.bullet("400 WEAK_PIN / PIN_LENGTH_ERROR")
    pdf.bullet("Then MUST call login (no tokens from this step)")

    pdf.h2("A5) POST /api/v1/newUsers/login")
    pdf.code(
        '{\n'
        '  "phoneNumber": "+994501234567",\n'
        '  "pinCode": "2580",\n'
        '  "deviceId": "<stable device id or FCM>",\n'
        '  "platform": "ANDROID" | "IOS"\n'
        "}"
    )
    pdf.bullet("deviceId required (alias deviceToken accepted)")
    pdf.bullet("401 PIN_INCORRECT / PIN_NOT_SET; 429 LOGIN_LOCKED (~300s)")
    pdf.bullet("Each login APPENDS refresh_tokens row (history); not upsert")

    pdf.h2("A6) Refresh (legacy endpoint)")
    pdf.p("POST /api/v1/users/refresh with Authorization: Bearer <refreshToken>")

    pdf.add_page()
    pdf.h2("B) Error envelope")
    pdf.code(
        '{\n'
        '  "error": "LOGIN_LOCKED",\n'
        '  "message": "localized text",\n'
        '  "timeStamp": "...",\n'
        '  "status": 429,\n'
        '  "lockedUntil": "...",\n'
        '  "remainingSeconds": 300,\n'
        '  "retryAfter": 300\n'
        "}"
    )
    pdf.h3("Named errors to handle")
    pdf.bullet("AUTH_TOKEN_EXPIRED / INVALID_TOKEN → restart from phone")
    pdf.bullet("OTP_EXPIRED / OTP_INCORRECT / OTP_VERIFY_LOCKED")
    pdf.bullet("WEAK_PIN / PIN_LENGTH_ERROR")
    pdf.bullet("PIN_INCORRECT / PIN_NOT_SET / LOGIN_LOCKED")

    pdf.h2("C) Legacy — keep for old app")
    pdf.bullet("Paths unchanged: login, register, otp/*, setPassword, updatePassword, refresh")
    pdf.bullet("setPassword = free-form password (BCrypt) — NOT 4-digit PIN rules")
    pdf.bullet("Legacy login may return 429 LOGIN_LOCKED after 3 wrong credentials")
    pdf.bullet("Forgot password still: updatePassword → OTP → setPassword")

    pdf.h2("D) Flutter checklist (NewUsers)")
    pdf.bullet("Persist authToken; replace after verify")
    pdf.bullet("Drive UI from next")
    pdf.bullet("Do not send purpose query on setPinCode")
    pdf.bullet("After PIN_SET always call login")
    pdf.bullet("Handle 429 with countdown UI")
    pdf.bullet("Never log OTP or PIN")

    pdf.ln(2)
    pdf.p("Source of truth: carland_auth NewUsersController + legacy UserController.")

    pdf.output(OUT)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
