package BankappJDBC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
// Program to facilitate fund transfer between two Accounts, one debit another credit

public class BankMain {
    public static Scanner scanner = new Scanner((System.in));
    public static String str;
    static int choice;
    static boolean loop = true;
    static Boolean isValid;
    static int primeacct = 0;
    static String name = null;
    static Connection conn = ConnectionFactory.getConnection();
    static PreparedStatement ps = null;
    static ResultSet rset = null;

    public static void main(String[] args) {
        System.out.println("\n----------------------------------");
        System.out.println("Welcome to the Bank App!  ");
        System.out.print("Please enter your account ID >> ");
        primeacct = scanner.nextInt();
        rset=checkAcct(primeacct);
        try {
            if (!rset.next()) {
                throw new InvalidAccountException("Invalid account ID entered.");
            }
            name = rset.getString(2);
        } catch (InvalidAccountException e) {
            loop=false;
            System.out.println(e.getMessage() + " Goodbye....");
        } catch (SQLException ex) {
            System.out.println("Server down. Goodbye...");
        }
        mainMenu(name);
    }

    private static void mainMenu(String name) {
        while (loop) {
            System.out.println("\n----------------------------------");
            System.out.println("Welcome " + name + ", to the new Banktransfer App!  ");
            System.out.println("Please select an option :");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer funds");
            System.out.println("5. Exit");
            System.out.print(">>> ");

            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    displaybalance(primeacct);
                    break;
                case 2:
                    deposit(primeacct);
                    break;
                case 3:
                    withdraw(primeacct);
                    break;
                case 4:
                    transferFunds(primeacct);
                    break;
                case 5:
                    System.out.println("Thank you for visiting, Goodbye !!!");
                    loop = false;
                    break;
                default:
                    System.out.println("Invalid input, retry : \n");
                    break;
            }
        }
    }

    public static void displaybalance(int primeacct) {
        double balance = getBalance(primeacct);
        System.out.println("Your current account balance is " + balance);
        continueYN();
    }

    public static void deposit(int primeacct) {
        double amount = 0;
        System.out.print("Enter Amount to deposit >>> ");
        amount = scanner.nextDouble();
        if (amount > 0) {
            try {
                conn.setAutoCommit(false);
                ps = conn.prepareStatement("update bankacct set balance=balance + ? where acct_id=?");
                ps.setDouble(1, amount);
                ps.setInt(2, primeacct);
                int rows = ps.executeUpdate();
                conn.commit();
                if (rows > 0) {
                    System.out.println("Deposit successful....");
                    displaybalance(primeacct);
                }
            } catch (SQLException e) {
                System.out.println("Error occurred during deposit." + e.getNextException().getMessage());
            }
        } else {
            System.out.println("Invalid Amount entered. Exiting...");
        }
    }

    public static void withdraw(int primeacct) {
        double amount = 0;
        System.out.print("Enter Amount to withdraw >>> ");
        amount = scanner.nextDouble();
        if (amount > 0) {
            try {
                double accountBal = getBalance(primeacct);

                if (amount <= accountBal) {
                    conn.setAutoCommit(false);
                    ps = conn.prepareStatement("update bankacct set balance=balance - ? where acct_id=?");
                    ps.setDouble(1, amount);
                    ps.setInt(2, primeacct);
                    int rows = ps.executeUpdate();
                    conn.commit();
                    if (rows > 0) {
                        System.out.println("Withdraw successful....");
                        displaybalance(primeacct);
                    } else {
                        System.out.println("Insufficient Balance. withdrawal failed!!! ");
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error occurred during withdraw transaction..." + e.getNextException().getMessage());
            }
        } else {
            System.out.println("Invalid Amount entered. Exiting...");
        }
    }

    public static void transferFunds(int primeacct) {
        double primebal = getBalance(primeacct);
        double amount = 0;
        boolean valAmt = true;
        boolean valAcct = true;
        PreparedStatement ps1;
        PreparedStatement ps2;

        System.out.print("Enter Recipient's 4dig account ID to transfer >>> ");
        int xfracct = scanner.nextInt();
        rset = checkAcct(xfracct);
        try {
            if (!rset.next()) {
                valAcct = false;
                loop=false;
                throw new InvalidAccountException("Invalid account ID entered. retry");

            }
            name = rset.getString(2);
        } catch (InvalidAccountException e) {
            System.out.println(e.getMessage() + " Goodbye....");
        } catch (SQLException ex) {
            System.out.println("Server down. Goodbye...");
        }
        if (valAcct) {
            System.out.print("Enter amount to be transferred >>> ");
            amount = scanner.nextDouble();
            if (amount < 1 || amount > primebal) {
                valAmt = false;
                System.out.println("Invalid Amount entered. transfer cancelled...");
            }
        }
        if (valAmt) {
            try {
                conn.setAutoCommit(false);
                ps1 = conn.prepareStatement("update bankacct set balance=balance - ? where acct_id=?");
                ps1.setDouble(1, amount);
                ps1.setInt(2, primeacct);
                int rows = ps1.executeUpdate();
                if (rows != 1){throw new SQLException();}

                ps2 = conn.prepareStatement("update bankacct set balance=balance + ? where acct_id=?");
                ps2.setDouble(1, amount);
                ps2.setInt(2, xfracct);
                rows = ps2.executeUpdate();
                if (rows != 1){throw new SQLException();}
                conn.commit();

                System.out.println("Transfer Successful, your current account balance is " + getBalance(primeacct));

            } catch (SQLException e) {
                System.out.println("Error occurred during withdraw transaction..." + e.getNextException().getMessage());
            }
        }
    }

    public static void continueYN() {
        String choice = null;
        System.out.print("Do you wish to continue? Y/N >> ");
        choice = scanner.next();
        if (choice.toLowerCase().equals("n")) {
            loop = false;
            System.out.println("Thank you for visiting, Goodbye !!!");
        }
    }

    public static double getBalance(int primeacct) {
        double balance = 0;
        try {
            ps = conn.prepareStatement("select balance from bankacct where acct_id = ?");
            ps.setInt(1, primeacct);
            ps.executeQuery();
            rset = ps.getResultSet();
            rset.next();
            balance = rset.getDouble(1);
        } catch (SQLException e) {
            System.out.println("Error occurred during balance check." + e.getNextException().getMessage());
        }
        return balance;
    }

    public static ResultSet checkAcct(int acct) {
        ResultSet rs=null;
        try {
            ps = conn.prepareStatement("select acct_id, name, balance from bankacct where acct_id=?");
            ps.setInt(1, acct);
            rs = ps.executeQuery();
        } catch (InvalidAccountException e) {
            System.out.println(e.getMessage() + " Goodbye....");
        } catch (SQLException ex) {
            System.out.println("Server down. Goodbye...");
        }
        return rs;
    }

}
//        try {
//            ps = conn.prepareStatement("select * from users");
//            rset = ps.executeQuery("select * from bankacct");
//        } catch (SQLException e) {
//            System.out.println(e);
//        }
//
//        try {
//             while (rset.next()) {
//                 System.out.println(rset.getInt("acct_id")
//                         + " / " + rset.getString("name")
//                         + " / " + rset.getString("balance"));
//
//             }
//            } catch (SQLException e) {
//                System.out.println(e);
//            }
//        }
//
//    }
