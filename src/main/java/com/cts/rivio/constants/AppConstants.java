package com.cts.rivio.constants;

public class AppConstants {

    private AppConstants() {}

    // ── Application URLs ──────────────────────────────────────────────────────
    public static final String BASE_URL          = "https://rivio-angular.vercel.app/";
    public static final String LOGIN_URL         = BASE_URL + "auth/login";
    public static final String DASHBOARD_URL     = BASE_URL + "dashboard";
    public static final String EMPLOYEE_DIR_URL  = BASE_URL + "employees";
    public static final String LEAVE_URL         = BASE_URL + "leave";
    public static final String ATTENDANCE_URL    = BASE_URL + "attendance";
    public static final String PAYROLL_URL       = BASE_URL + "payroll";
    public static final String RECRUITMENT_URL   = BASE_URL + "recruitment";
    public static final String JOB_BOARD_URL     = BASE_URL + "ats/job-board";
    public static final String COMPANY_URL       = BASE_URL + "company";

    // Self-service
    public static final String MY_PROFILE_URL    = BASE_URL + "self-service/my-profile";
    public static final String MY_LEAVES_URL     = BASE_URL + "self-service/my-leaves";
    public static final String MY_PAYSLIPS_URL   = BASE_URL + "self-service/my-payslips";
    public static final String MY_ATTENDANCE_URL = BASE_URL + "self-service/my-attendance";

    // ── Roles ─────────────────────────────────────────────────────────────────
    public static final String ROLE_SUPERADMIN      = "SUPERADMIN";
    public static final String ROLE_HR              = "HR";
    public static final String ROLE_MANAGER         = "MANAGER";
    public static final String ROLE_PAYROLL_MANAGER = "PAYROLL_MANAGER";
    public static final String ROLE_EMPLOYEE        = "EMPLOYEE";

    // ── Credentials (also in config.properties – kept here for quick reference) ──
    public static final String ADMIN_EMAIL     = "admin@rivio.com";
    public static final String ADMIN_PASSWORD  = "password";
    public static final String HR_EMAIL        = "hr@rivio.com";
    public static final String HR_PASSWORD     = "password";
    public static final String MANAGER_EMAIL   = "manager@rivio.com";
    public static final String MANAGER_PASSWORD= "password";
    public static final String PAYROLL_EMAIL   = "payroll@gmail.com";
    public static final String PAYROLL_PASSWORD= "password";
    public static final String EMPLOYEE_EMAIL  = "employee@rivio.com";
    public static final String EMPLOYEE_PASSWORD = "password";

    // ── Wait Timeouts (seconds) ───────────────────────────────────────────────
    public static final int IMPLICIT_WAIT    = 10;
    public static final int EXPLICIT_WAIT    = 30;
    public static final int PAGE_LOAD_WAIT   = 60;
    public static final int ANGULAR_WAIT     = 15;
    public static final int POLLING_INTERVAL = 500; // ms

    // ── File Paths (resolved to absolute at runtime via getAbsolutePath) ─────
    private static final String BASE_DIR = System.getProperty("user.dir");
    public static final String CONFIG_PATH       = BASE_DIR + "/src/test/resources/config.properties";
    public static final String REPORT_PATH       = BASE_DIR + "/src/test/resources/test-output/reports/ExtentReport.html";
    public static final String SCREENSHOT_PATH   = BASE_DIR + "/src/test/resources/test-output/screenshots/";
    public static final String LOGIN_DATA_PATH   = BASE_DIR + "/src/test/resources/testdata/LoginData.xlsx";
    public static final String EMPLOYEE_DATA_PATH= BASE_DIR + "/src/test/resources/testdata/EmployeeData.xlsx";
    public static final String LEAVE_DATA_PATH   = BASE_DIR + "/src/test/resources/testdata/LeaveData.xlsx";
    public static final String ATTENDANCE_DATA_PATH = BASE_DIR + "/src/test/resources/testdata/AttendanceData.xlsx";
    public static final String RECRUITMENT_DATA_PATH= BASE_DIR + "/src/test/resources/testdata/RecruitmentData.xlsx";

    // ── Excel Sheet Names ─────────────────────────────────────────────────────
    public static final String SHEET_VALID_LOGIN   = "ValidLogin";
    public static final String SHEET_INVALID_LOGIN = "InvalidLogin";
    public static final String SHEET_EMPLOYEE      = "EmployeeData";
    public static final String SHEET_LEAVE         = "LeaveData";
    public static final String SHEET_ATTENDANCE    = "AttendanceData";
    public static final String SHEET_RECRUITMENT   = "RecruitmentData";

    // ── Browser ───────────────────────────────────────────────────────────────
    public static final String BROWSER_CHROME  = "chrome";
    public static final String BROWSER_FIREFOX = "firefox";
    public static final String BROWSER_EDGE    = "edge";
}
