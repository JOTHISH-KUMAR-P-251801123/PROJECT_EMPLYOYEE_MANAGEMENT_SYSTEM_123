import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** EMS business logic (no database) + self-checks. Run: java EmsLogic.java */
public class EmsLogic {
    static int pass = 0, fail = 0;
    static void check(String name, Object expected, Object actual) {
        boolean ok = expected.toString().equals(actual.toString());
        if (ok) pass++; else fail++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + "  expected=" + expected + "  actual=" + actual);
    }
    static String f2(double v) { return String.format("%.2f", v); }

    public static void main(String[] args) {
        System.out.println("=== 1. OOP and polymorphism ===");
        Employee t = new TeachingStaff(2, "Arun Kumar", "Assistant Professor", "AI&DS", 50000);
        Employee n = new NonTeachingStaff(3, "Meena Devi", "Lab Assistant", "Administration", 30000);
        Person admin = new Admin(1, "Admin", "admin@ems.com");
        for (Employee e : List.of(t, n))
            System.out.println(e.getName() + " -> " + e.getCategory() + ", allowance " + f2(e.calculateAllowances()) + ", task limit " + e.getMaxTaskHours() + " h");
        check("Teaching task limit", 20, t.getMaxTaskHours());
        check("Non-teaching task limit", 30, n.getMaxTaskHours());
        check("Admin role", "ADMIN", admin.getRole());

        System.out.println("\n=== 2. Payroll calculation ===");
        Payroll p = Payroll.compute(t, 2);
        p.print(10, 2026);
        check("Teaching gross", "65000.00", f2(p.gross));
        check("Teaching PF", "6000.00", f2(p.pf));
        check("Teaching LOP (2 days)", "4333.33", f2(p.lop));
        check("Teaching net", "54666.67", f2(p.net));
        check("Non-teaching gross", "36600.00", f2(Payroll.compute(n, 0).gross));
        check("Non-teaching net, no LOP", "33000.00", f2(Payroll.compute(n, 0).net));
        check("Non-teaching LOP 1 day", "1220.00", f2(Payroll.compute(n, 1).lop));
        check("Non-teaching net, 1 LOP day", "31780.00", f2(Payroll.compute(n, 1).net));

        System.out.println("\n=== 3. AI performance score ===");
        check("Overall A=92,T=80,P=90", "86.80", f2(Rules.overall(92, 80, 90)));
        check("Attendance score (18,2,2,2)", "91.67", f2(Rules.attendanceScore(18, 2, 2, 2)));
        check("Task score 4 of 5", "80.00", f2(Rules.taskScore(4, 5)));
        check("Punctuality 18 present 2 late", "90.00", f2(Rules.punctuality(18, 2)));
        check("No attendance recorded", "0.00", f2(Rules.attendanceScore(0, 0, 0, 0)));
        check("No task due", "100.00", f2(Rules.taskScore(0, 0)));
        check("Rating 85", "Excellent", Rules.rating(85));
        check("Rating 84.99", "Good", Rules.rating(84.99));
        check("Rating 70", "Good", Rules.rating(70));
        check("Rating 69.99", "Average", Rules.rating(69.99));
        check("Rating 50", "Average", Rules.rating(50));
        check("Rating 49.99", "Needs Improvement", Rules.rating(49.99));

        System.out.println("\n=== 4. Workload analysis ===");
        check("Teaching 24 h pending", "OVERLOADED", Rules.workload(24, t.getMaxTaskHours()));
        check("Non-teaching 15 h pending", "BALANCED", Rules.workload(15, n.getMaxTaskHours()));
        check("Teaching 6 h pending", "UNDERLOADED", Rules.workload(6, t.getMaxTaskHours()));
        check("Exactly 100%", "BALANCED", Rules.workload(20, 20));
        check("Exactly 40%", "BALANCED", Rules.workload(8, 20));

        System.out.println("\n=== 5. Attendance risk ===");
        check("2 late/absent", "LOW", Rules.risk(2));
        check("3 late/absent", "MEDIUM", Rules.risk(3));
        check("4 late/absent", "MEDIUM", Rules.risk(4));
        check("5 late/absent", "HIGH", Rules.risk(5));

        System.out.println("\n=== 6. Task-assignment suggestion (greedy least-loaded) ===");
        List<Candidate> c = new ArrayList<>(List.of(
            new Candidate("Ravi S", 12, 20, 70), new Candidate("Divya M", 5, 20, 90), new Candidate("Karthik P", 5, 20, 80)));
        double newHours = 4;
        c.sort(Comparator.comparingDouble((Candidate x) -> x.after(newHours)).thenComparing(x -> -x.score));
        int r = 1;
        for (Candidate x : c) System.out.println("Rank " + r++ + ": " + x.name + "  load after = " + f2(x.after(newHours)) + "%  score = " + f2(x.score));
        check("Best candidate (tie by score)", "Divya M", c.get(0).name);
        check("Last candidate", "Ravi S", c.get(2).name);

        System.out.println("\n=== 7. Leave and attendance rules ===");
        check("Days 5th to 5th", 1, Rules.countDays(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 5)));
        check("Days 5th to 9th", 5, Rules.countDays(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 9)));
        check("Check-in 08:50", "PRESENT", Rules.statusFor(LocalTime.of(8, 50)));
        check("Check-in 09:15", "PRESENT", Rules.statusFor(LocalTime.of(9, 15)));
        check("Check-in 09:16", "LATE", Rules.statusFor(LocalTime.of(9, 16)));
        check("Overlap: 5-9 vs 8-12", true, Rules.overlaps(LocalDate.of(2026,11,5), LocalDate.of(2026,11,9), LocalDate.of(2026,11,8), LocalDate.of(2026,11,12)));
        check("Overlap: 5-9 vs 10-12", false, Rules.overlaps(LocalDate.of(2026,11,5), LocalDate.of(2026,11,9), LocalDate.of(2026,11,10), LocalDate.of(2026,11,12)));

        System.out.println("\nResult: " + pass + " passed, " + fail + " failed.");
    }
}

abstract class Person {
    protected final int id; protected final String name, email;
    Person(int id, String name, String email) { this.id = id; this.name = name; this.email = email; }
    public String getName() { return name; }
    public abstract String getRole();
}
class Admin extends Person {
    Admin(int id, String n, String e) { super(id, n, e); }
    public String getRole() { return "ADMIN"; }
}
abstract class Employee extends Person {
    protected final String designation, department; protected final double basic;
    Employee(int id, String n, String des, String dept, double basic) { super(id, n, ""); this.designation = des; this.department = dept; this.basic = basic; }
    public String getRole() { return "EMPLOYEE"; }
    public double getBasic() { return basic; }
    public abstract String getCategory();
    public abstract double calculateAllowances();
    public abstract int getMaxTaskHours();
    public double calculateGross() { return basic + basic * 0.10 + basic * 0.12 + calculateAllowances(); }
}
class TeachingStaff extends Employee {
    TeachingStaff(int id, String n, String d, String dept, double b) { super(id, n, d, dept, b); }
    public String getCategory() { return "TEACHING"; }
    public double calculateAllowances() { return basic * 0.08; }
    public int getMaxTaskHours() { return 20; }
}
class NonTeachingStaff extends Employee {
    NonTeachingStaff(int id, String n, String d, String dept, double b) { super(id, n, d, dept, b); }
    public String getCategory() { return "NON_TEACHING"; }
    public double calculateAllowances() { return 0; }
    public int getMaxTaskHours() { return 30; }
}
class Payroll {
    double basic, hra, da, allow, gross, pf, lop, net; int lopDays; String name; int id;
    static Payroll compute(Employee e, int lopDays) {
        Payroll p = new Payroll();
        p.id = e.id; p.name = e.getName(); p.basic = e.getBasic(); p.hra = p.basic * 0.10; p.da = p.basic * 0.12;
        p.allow = e.calculateAllowances(); p.gross = e.calculateGross(); p.pf = p.basic * 0.12;
        p.lopDays = lopDays; p.lop = Math.round(p.gross / 30 * lopDays * 100.0) / 100.0;
        p.net = p.gross - p.pf - p.lop; return p;
    }
    void print(int m, int y) {
        System.out.println("---------------------------------------------");
        System.out.println("PAYSLIP " + m + "/" + y);
        System.out.println("Employee : " + name + " (No. " + id + ")");
        System.out.println("Basic : " + EmsLogic.f2(basic));
        System.out.println("HRA (10%) : " + EmsLogic.f2(hra));
        System.out.println("DA (12%) : " + EmsLogic.f2(da));
        System.out.println("Academic allow. : " + EmsLogic.f2(allow));
        System.out.println("Gross salary : " + EmsLogic.f2(gross));
        System.out.println("PF (12%) : " + EmsLogic.f2(pf));
        System.out.println("LOP (" + lopDays + " day(s)) : " + EmsLogic.f2(lop));
        System.out.println("NET SALARY : " + EmsLogic.f2(net));
        System.out.println("---------------------------------------------");
    }
}
class Rules {
    static double attendanceScore(int present, int late, int onLeave, int absent) {
        int total = present + late + onLeave + absent;
        return total == 0 ? 0 : (present + late + onLeave) * 100.0 / total;
    }
    static double taskScore(int onTime, int due) { return due == 0 ? 100 : onTime * 100.0 / due; }
    static double punctuality(int present, int late) { return present + late == 0 ? 100 : present * 100.0 / (present + late); }
    static double overall(double a, double t, double p) { return 0.4 * a + 0.4 * t + 0.2 * p; }
    static String rating(double s) { return s >= 85 ? "Excellent" : s >= 70 ? "Good" : s >= 50 ? "Average" : "Needs Improvement"; }
    static String workload(double pending, int max) { double u = pending / max * 100; return u > 100 ? "OVERLOADED" : u >= 40 ? "BALANCED" : "UNDERLOADED"; }
    static String risk(int lateAbsent) { return lateAbsent >= 5 ? "HIGH" : lateAbsent >= 3 ? "MEDIUM" : "LOW"; }
    static long countDays(LocalDate s, LocalDate e) { return ChronoUnit.DAYS.between(s, e) + 1; }
    static String statusFor(LocalTime in) { return in.isAfter(LocalTime.of(9, 15)) ? "LATE" : "PRESENT"; }
    static boolean overlaps(LocalDate s1, LocalDate e1, LocalDate s2, LocalDate e2) { return !s1.isAfter(e2) && !e1.isBefore(s2); }
}
class Candidate {
    String name; double pending, max, score;
    Candidate(String n, double p, double m, double s) { name = n; pending = p; max = m; score = s; }
    double after(double h) { return (pending + h) / max * 100; }
}
