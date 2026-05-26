package com.cts.rivio.testdata;

import com.cts.rivio.constants.AppConstants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * GenerateTestData – regenerates every .xlsx file under
 * src/test/resources/testdata/ with comprehensive positive AND negative
 * data rows for every input field of the Rivio HRMS frontend.
 *
 * Each Excel file has TWO (or more) sheets:
 *   - Valid<Module>   : happy-path rows that the frontend MUST accept
 *   - Invalid<Module> : negative rows exercising each validation rule
 *
 * Run this once before executing the test suite:
 *   mvn test -DsuiteXmlFile=src/test/resources/suites/generate-data.xml
 *
 * Or directly:
 *   mvn test -Dtest=GenerateTestData
 *
 * Data design source-of-truth:
 *   - Rivio_FRD_Final_1.docx
 *   - Rivio_Angular-main frontend (form validators + bug list)
 *   - Manual QA findings (bank account, phone, weekend leaves, autofill, etc.)
 */
public class GenerateTestData {

    private static final String DIR = AppConstants.TESTDATA_DIR;

    // Header background colour (light blue)
    private static final byte[] HEADER_RGB = {(byte) 0xBD, (byte) 0xD7, (byte) 0xEE};
    private static final byte[] INVALID_HEADER_RGB = {(byte) 0xF8, (byte) 0xCB, (byte) 0xAD};

    @Test(groups = {"generate-data"})
    public void generateAllExcelFiles() throws IOException {
        Files.createDirectories(Paths.get(DIR));
        createLoginData();
        createEmployeeData();
        createAttendanceData();
        createLeaveData();
        createPayrollData();
        createRecruitmentData();
        System.out.println("[GenerateTestData] All 6 Excel files regenerated under " + DIR);
    }

    /** Allows direct execution via {@code java -cp ... GenerateTestData}. */
    public static void main(String[] args) throws Exception {
        new GenerateTestData().generateAllExcelFiles();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. LoginData.xlsx
    // ══════════════════════════════════════════════════════════════════════

    private void createLoginData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle headerStyle = headerStyle(wb, HEADER_RGB);
        CellStyle invalidHeaderStyle = headerStyle(wb, INVALID_HEADER_RGB);
        CellStyle dataStyle = dataStyle(wb);

        // ── Sheet 1: ValidLogin (5 roles) ────────────────────────────────
        Sheet valid = wb.createSheet(AppConstants.SHEET_VALID_LOGIN);
        writeHeader(valid, headerStyle, "email", "password", "role");
        writeRow(valid, 1, dataStyle, "admin@rivio.com",    "password", "SUPER_ADMIN");
        writeRow(valid, 2, dataStyle, "hr@rivio.com",       "password", "HR");
        writeRow(valid, 3, dataStyle, "manager@rivio.com",  "password", "MANAGER");
        writeRow(valid, 4, dataStyle, "payroll@rivio.com",  "password", "PAYROLL_MANAGER");
        writeRow(valid, 5, dataStyle, "employee@rivio.com", "password", "EMPLOYEE");
        autoSize(valid, 3);

        // ── Sheet 2: InvalidLogin — exhaustive negatives ─────────────────
        Sheet invalid = wb.createSheet(AppConstants.SHEET_INVALID_LOGIN);
        writeHeader(invalid, invalidHeaderStyle, "testCase", "email", "password", "expectedError");
        int r = 1;
        writeRow(invalid, r++, dataStyle, "TC-IL-01 Wrong password for known email",
                 "admin@rivio.com", "WrongPass123!", "Invalid credentials");
        writeRow(invalid, r++, dataStyle, "TC-IL-02 Non-existent email",
                 "ghost@rivio.com", "password", "Invalid credentials");
        writeRow(invalid, r++, dataStyle, "TC-IL-03 Empty email",
                 "", "password", "Email is required");
        writeRow(invalid, r++, dataStyle, "TC-IL-04 Empty password",
                 "admin@rivio.com", "", "Password is required");
        writeRow(invalid, r++, dataStyle, "TC-IL-05 Both fields empty",
                 "", "", "Email and password are required");
        writeRow(invalid, r++, dataStyle, "TC-IL-06 Malformed email no @",
                 "adminrivio.com", "password", "Please enter a valid email");
        writeRow(invalid, r++, dataStyle, "TC-IL-07 Malformed email no domain",
                 "admin@", "password", "Please enter a valid email");
        writeRow(invalid, r++, dataStyle, "TC-IL-08 Malformed email no tld",
                 "admin@rivio", "password", "Please enter a valid email");
        writeRow(invalid, r++, dataStyle, "TC-IL-09 Email with spaces",
                 " admin@rivio.com ", "password", "Invalid email format");
        writeRow(invalid, r++, dataStyle, "TC-IL-10 SQL injection attempt",
                 "admin@rivio.com' OR '1'='1", "password", "Invalid credentials");
        writeRow(invalid, r++, dataStyle, "TC-IL-11 XSS attempt in email",
                 "<script>alert(1)</script>@rivio.com", "password", "Invalid email format");
        writeRow(invalid, r++, dataStyle, "TC-IL-12 Email > 254 chars",
                 repeat("a", 250) + "@x.co", "password", "Email too long");
        writeRow(invalid, r++, dataStyle, "TC-IL-13 Password too short",
                 "admin@rivio.com", "abc", "Invalid credentials");
        writeRow(invalid, r++, dataStyle, "TC-IL-14 Password > 100 chars",
                 "admin@rivio.com", repeat("a", 105), "Invalid credentials");
        writeRow(invalid, r++, dataStyle, "TC-IL-15 Uppercase email (case sensitive backend)",
                 "ADMIN@RIVIO.COM", "password", "Invalid credentials");
        writeRow(invalid, r++, dataStyle, "TC-IL-16 Email with leading whitespace",
                 "  admin@rivio.com", "password", "Invalid email format");
        autoSize(invalid, 4);

        save(wb, "LoginData.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. EmployeeData.xlsx — Onboard + BankAccount + Phone
    // ══════════════════════════════════════════════════════════════════════

    private void createEmployeeData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb, HEADER_RGB);
        CellStyle invHStyle = headerStyle(wb, INVALID_HEADER_RGB);
        CellStyle dStyle = dataStyle(wb);

        // ── Sheet 1: ValidOnboard ────────────────────────────────────────
        // Columns match EmployeeTest.RV_EMP_DD_001 signature:
        //   firstName, lastName, email, tempPassword, employeeCode,
        //   systemRole, department, designation, location, reportsTo,
        //   employmentType, joiningDate
        // "AUTO" sentinel → page object selects the first option in the dropdown.
        // "today" sentinel → page object resolves to today's date.
        //
        // Dropdown values MUST match the live database options exactly:
        //   systemRole     ∈ Employee, Hr, Manager, Payroll Manager, Super Admin
        //   department     ∈ Administration, Human Resources, Engineering,
        //                    Finance, Marketing, healthcare
        //   employmentType ∈ Full Time, Part Time, Contract
        //   reportsTo      → ALWAYS blank (optional field, skipped by page object)
        //
        // Department → Designation cascade in DB:
        //   Administration   → System Administrator
        //   Human Resources  → HR Manager
        //   Engineering      → Engineering Lead, Software Developer, qea, SDET
        //   Finance          → Payroll Specialist
        //   Marketing        → Marketing Lead
        //   healthcare       → (no roles defined yet — avoid in valid data)
        // NO "AUTO" sentinel — designation must match its department per DB
        // cascade (see header comment). Location is one of the 5 DB values.
        Sheet vo = wb.createSheet(AppConstants.SHEET_VALID_ONBOARD);
        writeHeader(vo, hStyle, "firstName", "lastName", "email", "tempPassword",
                "employeeCode", "systemRole", "department", "designation",
                "location", "reportsTo", "employmentType", "joiningDate");
        int r = 1;
        writeRow(vo, r++, dStyle, "Alice", "Walker", uniq("alice.walker"),
                "Temp@1234", code("AW"), "Employee",
                "Engineering", "Engineering Lead", "Chennai HQ",
                "", "Full Time", "today");
        writeRow(vo, r++, dStyle, "Bob", "Chen", uniq("bob.chen"),
                "Temp@5678", code("BC"), "Employee",
                "Engineering", "Software Developer", "Bangalore Hub",
                "", "Part Time", "today");
        writeRow(vo, r++, dStyle, "Carol", "Dias", uniq("carol.dias"),
                "Temp@9012", code("CD"), "Manager",
                "Marketing", "Marketing Lead", "Delhi NCR",
                "", "Contract", "today");
        writeRow(vo, r++, dStyle, "David", "Ng", uniq("david.ng"),
                "Strong@P@ss1", code("DN"), "Employee",
                "Finance", "Payroll Specialist", "Bangalore",
                "", "Full Time", "today");
        writeRow(vo, r++, dStyle, "Eve", "Reyes", uniq("eve.reyes"),
                "Temp@3456", code("ER"), "Hr",
                "Human Resources", "HR Manager", "Chennai HQ",
                "", "Full Time", "today");
        writeRow(vo, r++, dStyle, "Frank", "Ito", uniq("frank.ito"),
                "Temp@7890", code("FI"), "Employee",
                "Administration", "System Administrator", "NAVI MUMBAI",
                "", "Contract", "today");
        autoSize(vo, 12);

        // ── Sheet 2: InvalidOnboard ──────────────────────────────────────
        // Columns: testCase + 12 form fields + expectedError = 14 columns
        Sheet io = wb.createSheet(AppConstants.SHEET_INVALID_ONBOARD);
        writeHeader(io, invHStyle, "testCase", "firstName", "lastName", "email",
                "tempPassword", "employeeCode", "systemRole", "department",
                "designation", "location", "reportsTo", "employmentType",
                "joiningDate", "expectedError");
        r = 1;
        writeRow(io, r++, dStyle, "TC-IO-01 Blank first name",
                "", "Smith", uniq("blank.fn"), "Temp@1234", code("X1"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "First name is required");
        writeRow(io, r++, dStyle, "TC-IO-02 Blank last name",
                "Dave", "", uniq("blank.ln"), "Temp@1234", code("X2"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Last name is required");
        writeRow(io, r++, dStyle, "TC-IO-03 Blank email",
                "Dave", "Smith", "", "Temp@1234", code("X3"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Email is required");
        writeRow(io, r++, dStyle, "TC-IO-04 Email no @ symbol",
                "Eve", "Jones", "notanemail", "Temp@1234", code("X4"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Please enter a valid email");
        writeRow(io, r++, dStyle, "TC-IO-05 Email no domain",
                "Eve", "Jones", "eve@", "Temp@1234", code("X5"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Please enter a valid email");
        writeRow(io, r++, dStyle, "TC-IO-06 Email with spaces",
                "Eve", "Jones", "eve jones@rivio.com", "Temp@1234", code("X6"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Invalid email format");
        writeRow(io, r++, dStyle, "TC-IO-07 Password too short (<6)",
                "Frank", "Lee", uniq("frank.short"), "123", code("X7"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Password must be at least 6 characters");
        writeRow(io, r++, dStyle, "TC-IO-08 Password blank",
                "Frank", "Lee", uniq("frank.nopass"), "", code("X8"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Password is required");
        writeRow(io, r++, dStyle, "TC-IO-09 Blank employee code",
                "Grace", "Kim", uniq("grace.nocode"), "Temp@1234", "",
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Employee code is required");
        writeRow(io, r++, dStyle, "TC-IO-10 Duplicate employee code (reuses EMP-AW-01)",
                "Hank", "Wu", uniq("hank.dup"), "Temp@1234", "EMP-AW-01",
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Employee code already exists");
        writeRow(io, r++, dStyle, "TC-IO-11 No department selected",
                "Ivy", "Park", uniq("ivy.nodept"), "Temp@1234", code("X11"),
                "Employee", "", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Department is required");
        writeRow(io, r++, dStyle, "TC-IO-12 No designation",
                "Jack", "Ho", uniq("jack.nodes"), "Temp@1234", code("X12"),
                "Employee", "Engineering", "", "Chennai HQ", "", "Full Time", "today",
                "Designation is required");
        writeRow(io, r++, dStyle, "TC-IO-13 No location",
                "Kate", "Rao", uniq("kate.noloc"), "Temp@1234", code("X13"),
                "Employee", "Engineering", "Engineering Lead", "", "", "Full Time", "today",
                "Location is required");
        writeRow(io, r++, dStyle, "TC-IO-14 No employment type",
                "Liam", "Cox", uniq("liam.notype"), "Temp@1234", code("X14"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "", "today",
                "Employment type is required");
        writeRow(io, r++, dStyle, "TC-IO-15 No joining date",
                "Mia", "Vu", uniq("mia.nodate"), "Temp@1234", code("X15"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "",
                "Joining date is required");
        writeRow(io, r++, dStyle, "TC-IO-16 Name with digits (special chars)",
                "Dave123", "Smith", uniq("dave.digits"), "Temp@1234", code("X16"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Names cannot contain digits");
        writeRow(io, r++, dStyle, "TC-IO-17 Name with special characters",
                "D@ve!", "Sm!th", uniq("dave.spc"), "Temp@1234", code("X17"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Names cannot contain special characters");
        writeRow(io, r++, dStyle, "TC-IO-18 Very long first name (>50 chars)",
                repeat("A", 60), "Smith", uniq("long.fn"), "Temp@1234", code("X18"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "First name must be 50 chars or less");
        writeRow(io, r++, dStyle, "TC-IO-19 SQL injection in first name",
                "Robert'); DROP TABLE--", "Smith", uniq("rob.sql"), "Temp@1234", code("X19"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Invalid characters in name");
        writeRow(io, r++, dStyle, "TC-IO-20 Email already registered",
                "Nick", "Tan", "admin@rivio.com", "Temp@1234", code("X20"),
                "Employee", "Engineering", "Engineering Lead", "Chennai HQ", "", "Full Time", "today",
                "Email already exists");
        autoSize(io, 14);

        // ── Sheet 3: ValidBankAccount — one happy-path bank value ────────
        Sheet vBank = wb.createSheet(AppConstants.SHEET_VALID_BANK);
        writeHeader(vBank, hStyle, "testCase", "bankAccountValue");
        writeRow(vBank, 1, dStyle, "TC-VB-01 Standard 12-digit numeric", "123456789012");
        autoSize(vBank, 2);

        // ── Sheet 4: InvalidBankAccount — one of each violation type ─────
        // Frontend currently has NO validator on bankAccount field.
        // Each row is an example of a value the field MUST reject after fix.
        Sheet bank = wb.createSheet(AppConstants.SHEET_INVALID_BANK);
        writeHeader(bank, invHStyle, "testCase", "bankAccountValue", "expectedError");
        r = 1;
        writeRow(bank, r++, dStyle, "TC-BANK-01 Alphabetic / random string",
                "abcd_xyz!!", "Bank account must be numeric");
        writeRow(bank, r++, dStyle, "TC-BANK-02 Too short (<9 digits)",
                "12345678", "Bank account must be at least 9 digits");
        writeRow(bank, r++, dStyle, "TC-BANK-03 Too long (>18 digits)",
                "1234567890123456789", "Bank account must be at most 18 digits");
        writeRow(bank, r++, dStyle, "TC-BANK-04 Empty value",
                "", "Bank account is required");
        autoSize(bank, 3);

        // ── Sheet 5: ValidPhone — one happy-path phone value ─────────────
        Sheet vPhone = wb.createSheet(AppConstants.SHEET_VALID_PHONE);
        writeHeader(vPhone, hStyle, "testCase", "phoneValue");
        writeRow(vPhone, 1, dStyle, "TC-VP-01 Standard 10-digit Indian", "9876543210");
        autoSize(vPhone, 2);

        // ── Sheet 6: InvalidPhone — one of each violation type ───────────
        Sheet phone = wb.createSheet(AppConstants.SHEET_INVALID_PHONE);
        writeHeader(phone, invHStyle, "testCase", "phoneValue", "expectedError");
        r = 1;
        writeRow(phone, r++, dStyle, "TC-PH-01 Alphabetic only",
                "abcdefghij", "Phone must be numeric");
        writeRow(phone, r++, dStyle, "TC-PH-02 Too short (<10 digits)",
                "98765", "Phone must be exactly 10 digits");
        writeRow(phone, r++, dStyle, "TC-PH-03 Too long (>10 digits)",
                "987654321012", "Phone must be exactly 10 digits");
        writeRow(phone, r++, dStyle, "TC-PH-04 Empty value",
                "", "Phone is required");
        autoSize(phone, 3);

        save(wb, "EmployeeData.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. AttendanceData.xlsx
    // ══════════════════════════════════════════════════════════════════════

    private void createAttendanceData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb, HEADER_RGB);
        CellStyle invHStyle = headerStyle(wb, INVALID_HEADER_RGB);
        CellStyle dStyle = dataStyle(wb);

        // ── Sheet 1: ValidPunch — one row per common scenario ────────────
        // Columns: employee, date, punchIn, punchOut
        // Sentinels:
        //   employee="AUTO"  → first employee in the searchable dropdown
        //   date="today"     → keeps the modal's default (today's date)
        //   times are "hh:mm AM/PM" — PrimeNG p-datepicker timeOnly hourFormat="12"
        Sheet vp = wb.createSheet(AppConstants.SHEET_VALID_PUNCH);
        writeHeader(vp, hStyle, "employee", "date", "punchIn", "punchOut");
        int r = 1;
        writeRow(vp, r++, dStyle, "AUTO", "today",     "09:00 AM", "06:00 PM");
        writeRow(vp, r++, dStyle, "AUTO", "yesterday", "08:30 AM", "05:30 PM");
        autoSize(vp, 4);

        // ── Sheet 2: InvalidPunch — 4 essential rejection cases ──────────
        Sheet ip = wb.createSheet(AppConstants.SHEET_INVALID_PUNCH);
        writeHeader(ip, invHStyle, "testCase", "employee", "date", "punchIn", "punchOut", "expectedError");
        r = 1;
        writeRow(ip, r++, dStyle, "TC-IP-01 No employee selected",
                "", "today", "09:00 AM", "06:00 PM", "Employee is required");
        writeRow(ip, r++, dStyle, "TC-IP-02 Missing punch-in time",
                "AUTO", "today", "", "06:00 PM",
                "Please provide at least a Punch In time");
        writeRow(ip, r++, dStyle, "TC-IP-03 Missing both punch times (not absent)",
                "AUTO", "today", "", "", "Please provide at least a Punch In time");
        writeRow(ip, r++, dStyle, "TC-IP-04 Punch-out before punch-in",
                "AUTO", "today", "06:00 PM", "09:00 AM", "Punch-out must be after punch-in");
        autoSize(ip, 6);

        save(wb, "AttendanceData.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. LeaveData.xlsx
    // ══════════════════════════════════════════════════════════════════════

    private void createLeaveData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb, HEADER_RGB);
        CellStyle invHStyle = headerStyle(wb, INVALID_HEADER_RGB);
        CellStyle dStyle = dataStyle(wb);

        // ── Sheet 1: ValidLeave ──────────────────────────────────────────
        // Columns match MyLeavesTest.RV_LVE_DD_001 signature:
        //   testCase, leaveType, startDaysFromToday, endDaysFromToday, reason
        // startDaysFromToday / endDaysFromToday are working-day offsets from
        // today; the page object skips Sat/Sun automatically.
        // One row per leave type — keeps the suite fast and predictable;
        // each row exercises the full Apply → Submit → History flow.
        Sheet vl = wb.createSheet(AppConstants.SHEET_VALID_LEAVE);
        writeHeader(vl, hStyle, "testCase", "leaveType", "startDaysFromToday",
                "endDaysFromToday", "reason");
        int r = 1;
        writeRow(vl, r++, dStyle, "TC-VL-01 Sick Leave one day",
                "Sick Leave", "3", "3", "");
        writeRow(vl, r++, dStyle, "TC-VL-02 Casual Leave two days",
                "Casual Leave", "5", "6", "");
        writeRow(vl, r++, dStyle, "TC-VL-03 Earned Leave three days",
                "Earned Leave", "8", "10", "");
        autoSize(vl, 5);

        // ── Sheet 2: InvalidLeave ────────────────────────────────────────
        // Columns: testCase, leaveType, startDaysFromToday, endDaysFromToday, reason, expectedError
        Sheet il = wb.createSheet(AppConstants.SHEET_INVALID_LEAVE);
        writeHeader(il, invHStyle, "testCase", "leaveType", "startDaysFromToday",
                "endDaysFromToday", "reason", "expectedError");
        r = 1;
        writeRow(il, r++, dStyle, "TC-IL-01 No leave type selected with valid dates",
                "", "5", "6", "Reason",
                "leaveTypeId required - Submit button must stay disabled");
        writeRow(il, r++, dStyle, "TC-IL-02 Leave type selected no date range",
                "Sick Leave", "", "", "Reason",
                "dateRange required - Submit button must stay disabled");
        writeRow(il, r++, dStyle, "TC-IL-03 Empty form both fields missing",
                "", "", "", "",
                "Both leaveTypeId and dateRange required - Submit disabled");
        writeRow(il, r++, dStyle, "TC-IL-04 Casual Leave start only no end",
                "Casual Leave", "5", "", "Reason",
                "Incomplete date range - form stays invalid");
        writeRow(il, r++, dStyle, "TC-IL-05 Sick Leave past date yesterday",
                "Sick Leave", "-1", "-1", "Backdated request",
                "Past date disabled in calendar - dateRange stays null");
        writeRow(il, r++, dStyle, "TC-IL-06 Casual Leave 3 past working days",
                "Casual Leave", "-3", "-1", "Backdated request",
                "Past dates are not selectable - calendar disables them");
        writeRow(il, r++, dStyle, "TC-IL-07 Earned Leave 5 past working days",
                "Earned Leave", "-5", "-1", "Backdated request",
                "Past dates are not selectable - calendar disables them");
        writeRow(il, r++, dStyle, "TC-IL-08 End date before start date",
                "Sick Leave", "5", "3", "Reversed range",
                "End date before start - PrimeNG resets selection, form stays invalid");
        writeRow(il, r++, dStyle, "TC-IL-09 Sick Leave exceeds remaining balance (20 days)",
                "Sick Leave", "1", "25", "Long sickness",
                "Exceeds Sick Leave available balance - server/UI must reject");
        writeRow(il, r++, dStyle, "TC-IL-10 Casual Leave exceeds remaining balance (20 days)",
                "Casual Leave", "1", "25", "Long break",
                "Exceeds Casual Leave available balance - server/UI must reject");
        writeRow(il, r++, dStyle, "TC-IL-11 Weekend selected in range (BUG: weekends counted)",
                "Casual Leave", "5", "11", "Range over weekend",
                "Weekend days must be excluded from leave count - per FRD 2.5");
        writeRow(il, r++, dStyle, "TC-IL-12 Reason exceeds max length (>500 chars)",
                "Sick Leave", "3", "3", repeat("a", 510),
                "Reason must be 500 chars or less");
        autoSize(il, 6);

        save(wb, "LeaveData.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. PayrollData.xlsx
    // ══════════════════════════════════════════════════════════════════════

    private void createPayrollData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb, HEADER_RGB);
        CellStyle invHStyle = headerStyle(wb, INVALID_HEADER_RGB);
        CellStyle dStyle = dataStyle(wb);

        // ── Sheet 1: ValidSalaryComponent ────────────────────────────────
        // Columns match PayrollTest.RV_PAY_DD_001 signature:
        //   employee, componentName, componentType, value
        Sheet vs = wb.createSheet(AppConstants.SHEET_VALID_SALARY);
        writeHeader(vs, hStyle, "employee", "componentName", "componentType", "value");
        int r = 1;
        writeRow(vs, r++, dStyle, "AUTO", "Basic Salary",         "Earning",   "50000");
        writeRow(vs, r++, dStyle, "AUTO", "House Rent Allowance", "Earning",   "15000");
        writeRow(vs, r++, dStyle, "AUTO", "Transport Allowance",  "Earning",    "3000");
        writeRow(vs, r++, dStyle, "AUTO", "Medical Allowance",    "Earning",    "1250");
        writeRow(vs, r++, dStyle, "AUTO", "Provident Fund",       "Deduction",  "6000");
        writeRow(vs, r++, dStyle, "AUTO", "Professional Tax",     "Deduction",   "200");
        writeRow(vs, r++, dStyle, "AUTO", "Income Tax",           "Deduction",  "5000");
        writeRow(vs, r++, dStyle, "AUTO", "Performance Bonus",    "Earning",   "10000");
        autoSize(vs, 4);

        // ── Sheet 2: InvalidSalaryComponent ──────────────────────────────
        Sheet is = wb.createSheet(AppConstants.SHEET_INVALID_SALARY);
        writeHeader(is, invHStyle, "testCase", "employee", "componentName",
                "componentType", "value", "expectedError");
        r = 1;
        writeRow(is, r++, dStyle, "TC-IS-01 Blank component name",
                "AUTO", "", "Earning", "50000", "Component name is required");
        writeRow(is, r++, dStyle, "TC-IS-02 Negative value",
                "AUTO", "Base Pay", "Earning", "-100", "Value must be a positive number");
        writeRow(is, r++, dStyle, "TC-IS-03 No component type",
                "AUTO", "Bonus", "", "5000", "Component type is required");
        writeRow(is, r++, dStyle, "TC-IS-04 Non-numeric value",
                "AUTO", "Allowance", "Earning", "abc", "Value must be numeric");
        writeRow(is, r++, dStyle, "TC-IS-05 Zero value",
                "AUTO", "Free Lunch", "Earning", "0", "Value must be greater than zero");
        writeRow(is, r++, dStyle, "TC-IS-06 Value with currency symbol",
                "AUTO", "HRA", "Earning", "₹15000", "Value must be a plain number");
        writeRow(is, r++, dStyle, "TC-IS-07 Decimal more than 2 places",
                "AUTO", "Allowance", "Earning", "1234.5678", "Value must have at most 2 decimal places");
        writeRow(is, r++, dStyle, "TC-IS-08 Component name with special chars",
                "AUTO", "B@s!c", "Earning", "5000", "Component name has invalid characters");
        writeRow(is, r++, dStyle, "TC-IS-09 Component name too long (>50 chars)",
                "AUTO", repeat("X", 60), "Earning", "5000", "Component name too long");
        writeRow(is, r++, dStyle, "TC-IS-10 Value extremely large (>10 crores)",
                "AUTO", "Mega Bonus", "Earning", "999999999", "Value exceeds maximum allowed");
        writeRow(is, r++, dStyle, "TC-IS-11 No employee selected",
                "", "Basic Salary", "Earning", "50000", "Employee must be selected first");
        writeRow(is, r++, dStyle, "TC-IS-12 Duplicate component (Basic Salary added twice)",
                "AUTO", "Basic Salary", "Earning", "60000", "Component already exists for employee");
        autoSize(is, 6);

        save(wb, "PayrollData.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 6. RecruitmentData.xlsx
    // ══════════════════════════════════════════════════════════════════════

    private void createRecruitmentData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb, HEADER_RGB);
        CellStyle invHStyle = headerStyle(wb, INVALID_HEADER_RGB);
        CellStyle dStyle = dataStyle(wb);

        // ── Sheet 1: ValidJobOpening — new job requisitions ──────────────
        // Columns: testCase, title, department, location
        // Title carries {RUN} so re-runs of the same row create a NEW
        // requisition each time (server allows duplicate titles, but unique
        // values keep the search assertion deterministic).
        // Dropdown values MUST match real DB:
        //   department ∈ Administration, Human Resources, Engineering,
        //                Finance, Marketing
        //   location   ∈ Chennai HQ, Bangalore Hub, Delhi NCR, Bangalore,
        //                NAVI MUMBAI
        Sheet vj = wb.createSheet(AppConstants.SHEET_VALID_JOB);
        writeHeader(vj, hStyle, "testCase", "title", "department", "location");
        int r = 1;
        writeRow(vj, r++, dStyle, "TC-VJ-01 Senior Frontend Developer",
                "Senior Frontend Developer {RUN}", "Engineering", "Chennai HQ");
        writeRow(vj, r++, dStyle, "TC-VJ-02 Marketing Specialist",
                "Marketing Specialist {RUN}", "Marketing", "Delhi NCR");
        autoSize(vj, 4);

        // ── Sheet 2: InvalidJobOpening ───────────────────────────────────
        Sheet ij = wb.createSheet(AppConstants.SHEET_INVALID_JOB);
        writeHeader(ij, invHStyle, "testCase", "title", "department",
                "location", "expectedError");
        r = 1;
        writeRow(ij, r++, dStyle, "TC-IJ-01 Blank job title",
                "", "Engineering", "Chennai HQ", "Job title is required");
        writeRow(ij, r++, dStyle, "TC-IJ-02 Blank department",
                "Backend Engineer {RUN}", "", "Chennai HQ", "Department is required");
        writeRow(ij, r++, dStyle, "TC-IJ-03 Blank location",
                "Solar Engineer {RUN}", "Engineering", "", "Location is required");
        writeRow(ij, r++, dStyle, "TC-IJ-04 All fields blank",
                "", "", "", "All required fields must be filled");
        autoSize(ij, 5);

        // ── Sheet 3: ValidCandidate — slimmed to 2 rows ──────────────────
        // Columns match RecruitmentTest.RV_REC_DD_001 signature:
        //   name, email, jobOpening, resumeUrl, stage
        // {RUN} suffix is substituted at runtime so the same xlsx produces a
        // unique candidate each invocation. jobOpening="AUTO" picks the first
        // open role from the live DB — no need to create one first.
        Sheet vc = wb.createSheet(AppConstants.SHEET_VALID_CANDIDATE);
        writeHeader(vc, hStyle, "name", "email", "jobOpening", "resumeUrl", "stage");
        r = 1;
        writeRow(vc, r++, dStyle, "Arjun Mehta {RUN}", "arjun.mehta.{RUN}@email.com",
                "AUTO", "https://drive.google.com/resume1", "APPLIED");
        writeRow(vc, r++, dStyle, "Priya Nair {RUN}", "priya.nair.{RUN}@email.com",
                "AUTO", "https://drive.google.com/resume2", "APPLIED");
        autoSize(vc, 5);

        // ── Sheet 4: InvalidCandidate — slimmed to 4 essential cases ─────
        Sheet ic = wb.createSheet(AppConstants.SHEET_INVALID_CANDIDATE);
        writeHeader(ic, invHStyle, "testCase", "name", "email", "jobOpening",
                "resumeUrl", "stage", "expectedError");
        r = 1;
        writeRow(ic, r++, dStyle, "TC-ICN-01 Blank candidate name",
                "", "blank.name.{RUN}@email.com", "AUTO", "", "APPLIED",
                "Candidate name is required");
        writeRow(ic, r++, dStyle, "TC-ICN-02 Invalid email no @",
                "Sam Doe {RUN}", "not-valid-email", "AUTO", "", "APPLIED",
                "Please enter a valid email");
        writeRow(ic, r++, dStyle, "TC-ICN-03 Blank email",
                "John Roe {RUN}", "", "AUTO", "", "APPLIED",
                "Email is required");
        writeRow(ic, r++, dStyle, "TC-ICN-04 No job opening selected",
                "Sam Doe {RUN}", "sam.doe.{RUN}@email.com", "", "", "APPLIED",
                "Job opening is required");
        autoSize(ic, 7);

        save(wb, "RecruitmentData.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    private static CellStyle headerStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 11);
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(rgb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle dataStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        return s;
    }

    private static void writeHeader(Sheet sheet, CellStyle style, String... values) {
        writeRow(sheet, 0, style, values);
    }

    private static void writeRow(Sheet sheet, int rowIdx, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i] == null ? "" : values[i]);
            cell.setCellStyle(style);
        }
    }

    private static void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // Cap super-long auto sizes (long SQL injection strings inflate columns)
            int width = sheet.getColumnWidth(i);
            if (width > 80 * 256) sheet.setColumnWidth(i, 80 * 256);
        }
    }

    private static void save(XSSFWorkbook wb, String fileName) throws IOException {
        File out = new File(DIR + fileName);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            wb.write(fos);
        }
        wb.close();
        System.out.println("[GenerateTestData] Created: " + out.getAbsolutePath());
    }

    /**
     * Emits an email template containing the {RUN} placeholder. The test
     * runtime substitutes this with a unique nanosecond+random suffix so the
     * SAME xlsx file produces fresh, server-unique values on every run —
     * eliminating the "duplicate email" failure when re-executing tests.
     */
    private static String uniq(String prefix) {
        return prefix + ".{RUN}@rivio.com";
    }

    /**
     * Emits an employee-code template containing the {RUN} placeholder.
     * Substituted at runtime, same reason as uniq().
     */
    private static String code(String suffix) {
        return "EMP-" + suffix + "-{RUN}";
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder(s.length() * times);
        for (int i = 0; i < times; i++) sb.append(s);
        return sb.toString();
    }
}
