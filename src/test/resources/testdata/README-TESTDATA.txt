TEST DATA FILES – RIVIO AUTOMATION
====================================

Create the following Excel (.xlsx) files in this folder.
Each file must have the exact sheet names and column headers listed below.

──────────────────────────────────────────────────────────────────────────
1. LoginData.xlsx
──────────────────────────────────────────────────────────────────────────
Sheet: ValidLogin
Headers: email | password | role

Data rows:
admin@rivio.com     | password | SUPERADMIN
hr@rivio.com        | password | HR
manager@rivio.com   | password | MANAGER
payroll@gmail.com   | password | PAYROLL_MANAGER
employee@rivio.com  | password | EMPLOYEE

Sheet: InvalidLogin
Headers: email | password | expectedErrorMessage

Data rows:
admin@rivio.com     | wrongpass    | Invalid credentials
notexist@test.com   | password     | Invalid credentials
                    | password     | (empty)
admin@rivio.com     |              | (empty)
notanemail          | password     | (empty)


──────────────────────────────────────────────────────────────────────────
2. EmployeeData.xlsx
──────────────────────────────────────────────────────────────────────────
Sheet: EmployeeData
Headers: firstName | lastName | email | phone | dob | gender |
         department | designation | employmentType | joinDate | location | salary

Data rows (sample):
Alice  | Smith   | alice.smith@rivio.com   | 9876543210 | 1995-03-15 | Female | Engineering | Software Engineer | FULL_TIME | 2025-01-01 | Mumbai   | 80000
Bob    | Jones   | bob.jones@rivio.com     | 9123456789 | 1990-07-22 | Male   | HR          | HR Executive      | FULL_TIME | 2025-02-01 | Bangalore| 60000


──────────────────────────────────────────────────────────────────────────
3. LeaveData.xlsx
──────────────────────────────────────────────────────────────────────────
Sheet: LeaveData
Headers: status | fromDate | toDate | expectedCount

Data rows:
PENDING  | 2025-01-01 | 2025-12-31 |
APPROVED | 2025-01-01 | 2025-12-31 |
REJECTED | 2025-01-01 | 2025-12-31 |
         | 2025-06-01 | 2025-06-30 |


──────────────────────────────────────────────────────────────────────────
4. AttendanceData.xlsx
──────────────────────────────────────────────────────────────────────────
Sheet: AttendanceData
Headers: employeeName | fromDate | toDate | expectedRecords

Data rows:
John    | 2025-01-01 | 2025-01-31 |
        | 2025-05-01 | 2025-05-31 |


──────────────────────────────────────────────────────────────────────────
5. RecruitmentData.xlsx
──────────────────────────────────────────────────────────────────────────
Sheet: RecruitmentData
Headers: searchKeyword | expectedMinCount

Data rows:
Engineer  | 1
Designer  | 0
Manager   | 1
──────────────────────────────────────────────────────────────────────────

TIP: You can create these with Microsoft Excel or Apache POI.
     The ExcelUtils.java class handles reading; column order must match headers.
