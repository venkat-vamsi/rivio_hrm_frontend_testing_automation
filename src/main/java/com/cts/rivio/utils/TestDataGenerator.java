package com.cts.rivio.utils;

import com.cts.rivio.constants.AppConstants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * TestDataGenerator
 * ─────────────────
 * Generates all Excel test-data workbooks needed by the data-driven test suite.
 * Run this once (or from a @BeforeSuite hook) to create / refresh the files:
 *
 *   mvn exec:java -Dexec.mainClass="com.cts.rivio.utils.TestDataGenerator"
 *
 * Files created under src/test/resources/testdata/:
 *   EmployeeData.xlsx   — ValidOnboard / InvalidOnboard
 *   AttendanceData.xlsx — ValidPunch   / InvalidPunch
 *   LeaveData.xlsx      — ValidLeave   / InvalidLeave
 *   PayrollData.xlsx    — ValidSalaryComponent / InvalidSalaryComponent
 *   RecruitmentData.xlsx— ValidCandidate / InvalidCandidate
 *
 * COLUMN ORDERING matches the @DataProvider Object[][] parameter order in each
 * test class exactly — if you add/remove columns here, update the corresponding
 * @Test method signature too.
 *
 * "AUTO" sentinel value in dropdown columns → page object picks the first
 *   available option from the live UI (avoids hard-coding IDs that change).
 * "today"/"yesterday"/"today+N" in date columns → page object resolves at
 *   runtime so the test always targets a valid date regardless of when it runs.
 */
public class TestDataGenerator {

    public static void main(String[] args) throws IOException {
        new File(AppConstants.TESTDATA_DIR).mkdirs();
        generateEmployeeData();
        generateAttendanceData();
        generateLeaveData();
        generatePayrollData();
        generateRecruitmentData();
        System.out.println("[TestDataGenerator] All Excel files generated in: "
                + AppConstants.TESTDATA_DIR);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EmployeeData.xlsx
    // ══════════════════════════════════════════════════════════════════════════

    private static void generateEmployeeData() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── ValidOnboard sheet ──────────────────────────────────────────
            // Columns: firstName | lastName | email | tempPassword | employeeCode
            //          | systemRole | department | designation | location
            //          | reportsTo | employmentType | joiningDate
            Sheet valid = wb.createSheet(AppConstants.SHEET_VALID_ONBOARD);
            writeRow(valid, 0,
                "firstName","lastName","email","tempPassword","employeeCode",
                "systemRole","department","designation","location",
                "reportsTo","employmentType","joiningDate");

            writeRow(valid, 1,
                "Alice","Walker","alice.walker.test@rivio.com","Temp@1234","EMP-AW-01",
                "Employee","AUTO","AUTO","AUTO",
                "AUTO","Full-time","today");

            writeRow(valid, 2,
                "Bob","Chen","bob.chen.test@rivio.com","Temp@5678","EMP-BC-02",
                "Employee","AUTO","AUTO","AUTO",
                "AUTO","Part-time","today");

            writeRow(valid, 3,
                "Carol","Dias","carol.dias.test@rivio.com","Temp@9012","EMP-CD-03",
                "Employee","AUTO","AUTO","AUTO",
                "AUTO","Contract","today");

            // ── InvalidOnboard sheet ────────────────────────────────────────
            // Columns: testCase | firstName | lastName | email | tempPassword
            //          | employeeCode | systemRole | department | designation
            //          | location | reportsTo | employmentType | joiningDate | expectedError
            Sheet invalid = wb.createSheet(AppConstants.SHEET_INVALID_ONBOARD);
            writeRow(invalid, 0,
                "testCase","firstName","lastName","email","tempPassword",
                "employeeCode","systemRole","department","designation",
                "location","reportsTo","employmentType","joiningDate","expectedError");

            writeRow(invalid, 1,
                "Blank email",
                "Dave","Smith","","Temp@1234",
                "EMP-DS-01","Employee","AUTO","AUTO",
                "AUTO","AUTO","Full-time","today",
                "Email is required");

            writeRow(invalid, 2,
                "Invalid email format",
                "Eve","Jones","not-an-email","Temp@1234",
                "EMP-EJ-02","Employee","AUTO","AUTO",
                "AUTO","AUTO","Full-time","today",
                "Valid email required");

            writeRow(invalid, 3,
                "Weak password",
                "Frank","Lee","frank.lee.test@rivio.com","123",
                "EMP-FL-03","Employee","AUTO","AUTO",
                "AUTO","AUTO","Full-time","today",
                "Password too weak");

            writeRow(invalid, 4,
                "Blank first name",
                "","Taylor","blank.first@rivio.com","Temp@1234",
                "EMP-BF-04","Employee","AUTO","AUTO",
                "AUTO","AUTO","Full-time","today",
                "First name is required");

            writeRow(invalid, 5,
                "Duplicate employee code",
                "Grace","Kim","grace.kim.test@rivio.com","Temp@1234",
                "EMP-AW-01","Employee","AUTO","AUTO",
                "AUTO","AUTO","Full-time","today",
                "Employee code already exists");

            write(wb, AppConstants.EMPLOYEE_DATA_PATH);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AttendanceData.xlsx
    // ══════════════════════════════════════════════════════════════════════════

    private static void generateAttendanceData() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── ValidPunch sheet ────────────────────────────────────────────
            // Columns: employee | date | punchIn | punchOut
            // "AUTO" employee → select first available in dropdown
            // "today"/"yesterday" date → resolved at runtime
            Sheet valid = wb.createSheet(AppConstants.SHEET_VALID_PUNCH);
            writeRow(valid, 0, "employee","date","punchIn","punchOut");

            writeRow(valid, 1, "AUTO","today","09:00","18:00");
            writeRow(valid, 2, "AUTO","yesterday","08:30","17:30");
            writeRow(valid, 3, "AUTO","today","10:00","19:00");

            // ── InvalidPunch sheet ──────────────────────────────────────────
            // Columns: testCase | employee | date | punchIn | punchOut | expectedError
            Sheet invalid = wb.createSheet(AppConstants.SHEET_INVALID_PUNCH);
            writeRow(invalid, 0, "testCase","employee","date","punchIn","punchOut","expectedError");

            writeRow(invalid, 1,
                "Punch-out before punch-in",
                "AUTO","today","18:00","09:00",
                "Punch-out must be after punch-in");

            writeRow(invalid, 2,
                "No employee selected",
                "","today","09:00","18:00",
                "Employee is required");

            writeRow(invalid, 3,
                "Future date",
                "AUTO","today+7","09:00","18:00",
                "Future date not allowed");

            writeRow(invalid, 4,
                "Missing punch-in time",
                "AUTO","today","","18:00",
                "Punch-in time is required");

            write(wb, AppConstants.ATTENDANCE_DATA_PATH);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LeaveData.xlsx  –  comprehensive leave application test data
    // ══════════════════════════════════════════════════════════════════════════
    //
    // ── ACTUAL UI (confirmed from live Rivio DOM) ─────────────────────────────
    //   Form fields inside "Apply for Leave" dialog:
    //     1. Leave Type   → p-select[formcontrolname='leaveTypeId']  (REQUIRED)
    //        Options:  Sick Leave | Casual Leave | Earned Leave
    //     2. Date Range   → p-datepicker[formcontrolname='dateRange'
    //                        selectionMode='range' appendTo='body']   (REQUIRED)
    //        • Input is readonly="" — calendar clicks only, no keyboard entry
    //        • Calendar panel renders at BODY level (appendTo="body")
    //        • Non-working days and holidays are unselectable (per UI label)
    //        • Past dates are disabled (PrimeNG minDate guard)
    //     3. Working Days Requested → read-only counter (computed by form)
    //     ✗  NO reason / notes textarea in the current UI
    //
    //   Submit button: disabled="" when form is ng-invalid (both fields required)
    //
    // ── COLUMN GUIDE ─────────────────────────────────────────────────────────
    //   testCase           : label shown in Extent report
    //   leaveType          : "Sick Leave" | "Casual Leave" | "Earned Leave" | ""
    //                         "" → skip selectLeaveType() → form stays ng-invalid
    //   startDaysFromToday : working-day offset (+N future, -N past, "" = skip dates)
    //   endDaysFromToday   : same semantics
    //   reason             : KEPT for forward-compatibility; always empty because
    //                         the live UI has NO reason textarea.  fillReason() is
    //                         a no-op when no textarea is present.
    //
    // ── BALANCE (employee@rivio.com at test start) ────────────────────────────
    //   Sick Leave   :  9 available / 12 total
    //   Casual Leave :  9 available / 12 total
    //   Earned Leave : 14 available / 15 total
    //
    // ── POSITIVE DATE STRATEGY ────────────────────────────────────────────────
    //   Base = today (May 25 = Sunday).  resolveWorkingDay() skips Sat/Sun.
    //   Offset 1 = May 26 (Mon), offset 5 = May 30 (Fri), offset 6 = Jun 2 (Mon).
    //   Start from offset 6 to stay clear of the near-future dates that the
    //   demo environment already has leave entries for (May 26-30).
    //   NON-OVERLAPPING windows per leave type to avoid balance conflicts:
    //     Sick Leave   : offsets  6-12  → 1+2+3+1 = 7 days used  (≤ 9  ✓)
    //     Casual Leave : offsets 13-19  → 1+2+3+1 = 7 days used  (≤ 9  ✓)
    //     Earned Leave : offsets 20-28  → 1+2+5+1 = 9 days used  (≤ 14 ✓)

    private static void generateLeaveData() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ═══════════════════════════════════════════════════════════════
            // ValidLeave sheet  (12 positive rows)
            // ═══════════════════════════════════════════════════════════════
            // reason column is intentionally BLANK — the live Apply for Leave
            // modal has no reason / notes textarea (confirmed from DOM).
            // The column is retained for forward-compatibility; fillReason()
            // silently no-ops when no textarea element is found.
            // ═══════════════════════════════════════════════════════════════
            Sheet valid = wb.createSheet(AppConstants.SHEET_VALID_LEAVE);
            writeRow(valid, 0,
                "testCase", "leaveType",
                "startDaysFromToday", "endDaysFromToday", "reason");

            // ── SICK LEAVE (4 cases, offsets 6-12) ──────────────────────
            //   Working days consumed: 1 + 2 + 3 + 1 = 7  (balance = 9 ✓)

            // TC-V-01  Most basic scenario: single future working day
            writeRow(valid, 1,
                "TC-V-01 Sick Leave single day",
                "Sick Leave", "6", "6", "");

            // TC-V-02  Two consecutive working days
            writeRow(valid, 2,
                "TC-V-02 Sick Leave two consecutive days",
                "Sick Leave", "7", "8", "");

            // TC-V-03  Three working days (covers a Wed–Fri or Mon–Wed window)
            writeRow(valid, 3,
                "TC-V-03 Sick Leave three days",
                "Sick Leave", "9", "11", "");

            // TC-V-04  Single day — end = start (boundary: minimal range)
            writeRow(valid, 4,
                "TC-V-04 Sick Leave boundary single day offset 12",
                "Sick Leave", "12", "12", "");

            // ── CASUAL LEAVE (4 cases, offsets 13-19) ───────────────────
            //   Working days consumed: 1 + 2 + 3 + 1 = 7  (balance = 9 ✓)

            // TC-V-05  Single working day
            writeRow(valid, 5,
                "TC-V-05 Casual Leave single day",
                "Casual Leave", "13", "13", "");

            // TC-V-06  Two consecutive days
            writeRow(valid, 6,
                "TC-V-06 Casual Leave two days",
                "Casual Leave", "14", "15", "");

            // TC-V-07  Three working days
            writeRow(valid, 7,
                "TC-V-07 Casual Leave three days",
                "Casual Leave", "16", "18", "");

            // TC-V-08  Single day (boundary: minimal casual range)
            writeRow(valid, 8,
                "TC-V-08 Casual Leave boundary single day offset 19",
                "Casual Leave", "19", "19", "");

            // ── EARNED LEAVE (4 cases, offsets 20-28) ───────────────────
            //   Working days consumed: 1 + 2 + 5 + 1 = 9  (balance = 14 ✓)

            // TC-V-09  Single working day
            writeRow(valid, 9,
                "TC-V-09 Earned Leave single day",
                "Earned Leave", "20", "20", "");

            // TC-V-10  Two consecutive days
            writeRow(valid, 10,
                "TC-V-10 Earned Leave two days",
                "Earned Leave", "21", "22", "");

            // TC-V-11  Five working days — standard full-week annual leave block
            writeRow(valid, 11,
                "TC-V-11 Earned Leave five days full week",
                "Earned Leave", "23", "27", "");

            // TC-V-12  Single day (boundary: last earned window)
            writeRow(valid, 12,
                "TC-V-12 Earned Leave boundary single day offset 28",
                "Earned Leave", "28", "28", "");

            // ═══════════════════════════════════════════════════════════════
            // InvalidLeave sheet  (10 negative rows)
            // ═══════════════════════════════════════════════════════════════
            // "Rejected" = ANY ONE of these indicators fires:
            //   (a) Submit button disabled  → reactive-form guard (leaveTypeId /
            //       dateRange FormControl still null / invalid)
            //   (b) Inline validation error → .p-error or ng-invalid.ng-touched
            //   (c) Insufficient-balance warning → text containing "insufficient"
            //       / "exceed" / "balance" / "not enough"
            //   (d) Modal still open after submit → server rejected the request
            // ═══════════════════════════════════════════════════════════════
            Sheet invalid = wb.createSheet(AppConstants.SHEET_INVALID_LEAVE);
            writeRow(invalid, 0,
                "testCase", "leaveType",
                "startDaysFromToday", "endDaysFromToday",
                "reason", "expectedError");

            // ── GROUP 1: MISSING REQUIRED FIELDS ────────────────────────
            // The Apply for Leave form has exactly 2 required fields:
            //   leaveTypeId (p-select) and dateRange (p-datepicker).
            // Leaving either empty keeps the form ng-invalid → Submit disabled.

            // TC-I-01  No leave type — valid future dates provided
            //          FormControl leaveTypeId = null → ng-invalid → disabled
            writeRow(invalid, 1,
                "TC-I-01 No leave type selected with valid dates",
                "", "6", "7",
                "",
                "leaveTypeId required - Submit button must stay disabled");

            // TC-I-02  Leave type selected — no dates picked at all
            //          FormControl dateRange = null → ng-invalid → disabled
            writeRow(invalid, 2,
                "TC-I-02 Leave type selected no date range",
                "Sick Leave", "", "",
                "",
                "dateRange required - Submit button must stay disabled");

            // TC-I-03  Completely empty form — neither field touched
            //          Both validators fire → Submit disabled
            writeRow(invalid, 3,
                "TC-I-03 Empty form both fields missing",
                "", "", "",
                "",
                "Both leaveTypeId and dateRange required - Submit disabled");

            // TC-I-04  Leave type selected, only start date (no end)
            //          PrimeNG range mode: single click sets start only;
            //          the FormControl stays null until end is also clicked.
            //          Passing start=end=same forces resolveWorkingDay to give
            //          the same day twice — range selector may treat as 1 day
            //          or stay open waiting for end click.
            writeRow(invalid, 4,
                "TC-I-04 Casual Leave date range start only no end",
                "Casual Leave", "6", "",
                "",
                "Incomplete date range - form stays invalid");

            // ── GROUP 2: PAST DATES ──────────────────────────────────────
            // PrimeNG disables past day cells (minDate guard).
            // clickDayCell() skips elements whose td has data-p-disabled='true'
            // or the p-disabled class, so the click is silently dropped.
            // dateRange FormControl stays null → Submit stays disabled.

            // TC-I-05  Exactly yesterday — 1 working day in the past
            writeRow(invalid, 5,
                "TC-I-05 Sick Leave past date yesterday",
                "Sick Leave", "-1", "-1",
                "",
                "Past date disabled in calendar - dateRange stays null");

            // TC-I-06  Three working days in the past
            writeRow(invalid, 6,
                "TC-I-06 Casual Leave 3 past working days",
                "Casual Leave", "-3", "-1",
                "",
                "Past dates are not selectable - calendar disables them");

            // TC-I-07  5 working days in the past (deeper boundary check)
            writeRow(invalid, 7,
                "TC-I-07 Earned Leave 5 past working days",
                "Earned Leave", "-5", "-3",
                "",
                "Past dates are not selectable - calendar disables them");

            // ── GROUP 3: END DATE BEFORE START DATE ──────────────────────
            // setLeaveDateRange(start=8, end=6) → resolveWorkingDay gives
            // startDate (Jun 4) later than endDate (Jun 2).
            // After clicking Jun 4 first, clicking Jun 2 in range mode
            // causes PrimeNG to reset: Jun 2 becomes the new start.
            // The calendar stays open (waiting for end click); the code then
            // closes it via Escape. dateRange FormControl may be null or
            // contain only a half-range → submit stays disabled.
            writeRow(invalid, 8,
                "TC-I-08 Sick Leave end offset smaller than start offset",
                "Sick Leave", "8", "6",
                "",
                "End date before start - PrimeNG resets selection, form stays invalid");

            // ── GROUP 4: EXCEEDS AVAILABLE LEAVE BALANCE ─────────────────
            // Requests more days than the current available balance.
            // Expected: balance warning in the modal OR server rejects the
            // POST and modal stays open (both count as "rejected").
            //
            // Sick   available = 9  →  request 20 working days (offsets 6-25)
            // Casual available = 9  →  request 20 working days (offsets 6-25)
            //
            // NOTE: offsets 6-25 for BOTH sick and casual in the NEGATIVE
            // tests are INTENTIONALLY the same date range — the positive tests
            // use different leave-type-specific offsets (6-12 sick, 13-19 casual)
            // so the balance after positive tests is:
            //   Sick   remaining ≈ 9 - 7 = 2   →  request 20 still exceeds ✓
            //   Casual remaining ≈ 9 - 7 = 2   →  request 20 still exceeds ✓

            // TC-I-09  Sick Leave — 20 working days requested (balance ≈ 2 after +ve tests)
            writeRow(invalid, 9,
                "TC-I-09 Sick Leave exceeds remaining balance 20 days",
                "Sick Leave", "6", "25",
                "",
                "Exceeds Sick Leave available balance - server/UI must reject");

            // TC-I-10  Casual Leave — 20 working days requested (balance ≈ 2 after +ve tests)
            writeRow(invalid, 10,
                "TC-I-10 Casual Leave exceeds remaining balance 20 days",
                "Casual Leave", "6", "25",
                "",
                "Exceeds Casual Leave available balance - server/UI must reject");

            write(wb, AppConstants.LEAVE_DATA_PATH);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PayrollData.xlsx
    // ══════════════════════════════════════════════════════════════════════════

    private static void generatePayrollData() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── ValidSalaryComponent sheet ──────────────────────────────────
            // Columns: employee | componentName | componentType | value
            // "AUTO" employee → first employee in the payroll dropdown
            Sheet valid = wb.createSheet(AppConstants.SHEET_VALID_SALARY);
            writeRow(valid, 0, "employee","componentName","componentType","value");

            writeRow(valid, 1, "AUTO","Basic Salary","Earning","50000");
            writeRow(valid, 2, "AUTO","House Rent Allowance","Earning","15000");
            writeRow(valid, 3, "AUTO","Provident Fund","Deduction","6000");
            writeRow(valid, 4, "AUTO","Transport Allowance","Earning","3000");
            writeRow(valid, 5, "AUTO","Professional Tax","Deduction","200");

            // ── InvalidSalaryComponent sheet ───────────────────────────────
            // Columns: testCase | employee | componentName | componentType | value | expectedError
            Sheet invalid = wb.createSheet(AppConstants.SHEET_INVALID_SALARY);
            writeRow(invalid, 0,
                "testCase","employee","componentName","componentType","value","expectedError");

            writeRow(invalid, 1,
                "Blank component name",
                "AUTO","","Earning","50000",
                "Component name is required");

            writeRow(invalid, 2,
                "Negative value",
                "AUTO","Base Pay","Earning","-100",
                "Value must be a positive number");

            writeRow(invalid, 3,
                "No component type",
                "AUTO","Bonus","","5000",
                "Component type is required");

            writeRow(invalid, 4,
                "Non-numeric value",
                "AUTO","Allowance","Earning","abc",
                "Value must be numeric");

            write(wb, AppConstants.PAYROLL_DATA_PATH);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RecruitmentData.xlsx
    // ══════════════════════════════════════════════════════════════════════════

    private static void generateRecruitmentData() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── ValidCandidate sheet ────────────────────────────────────────
            // Columns: name | email | jobOpening | resumeUrl | stage
            // "AUTO" jobOpening → first available job opening in dropdown
            Sheet valid = wb.createSheet(AppConstants.SHEET_VALID_CANDIDATE);
            writeRow(valid, 0, "name","email","jobOpening","resumeUrl","stage");

            writeRow(valid, 1,
                "Arjun Mehta","arjun.mehta.test@email.com",
                "AUTO","https://drive.google.com/resume1","APPLIED");

            writeRow(valid, 2,
                "Priya Nair","priya.nair.test@email.com",
                "AUTO","https://drive.google.com/resume2","APPLIED");

            writeRow(valid, 3,
                "Ravi Sharma","ravi.sharma.test@email.com",
                "AUTO","https://linkedin.com/in/ravi","APPLIED");

            // ── InvalidCandidate sheet ──────────────────────────────────────
            // Columns: testCase | name | email | jobOpening | resumeUrl | stage | expectedError
            Sheet invalid = wb.createSheet(AppConstants.SHEET_INVALID_CANDIDATE);
            writeRow(invalid, 0,
                "testCase","name","email","jobOpening","resumeUrl","stage","expectedError");

            writeRow(invalid, 1,
                "Blank candidate name",
                "","test@email.com","AUTO","https://resume.url","APPLIED",
                "Candidate name is required");

            writeRow(invalid, 2,
                "Invalid email",
                "Sam Doe","not-valid-email","AUTO","https://resume.url","APPLIED",
                "Valid email required");

            writeRow(invalid, 3,
                "Blank email",
                "Sam Doe","","AUTO","https://resume.url","APPLIED",
                "Email is required");

            writeRow(invalid, 4,
                "No job opening selected",
                "Sam Doe","sam@email.com","","https://resume.url","APPLIED",
                "Job opening is required");

            write(wb, AppConstants.RECRUITMENT_DATA_PATH);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // helpers
    // ══════════════════════════════════════════════════════════════════════════

    private static void writeRow(Sheet sheet, int rowNum, String... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private static void write(XSSFWorkbook wb, String path) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        System.out.println("[TestDataGenerator] Written: " + path);
    }
}
