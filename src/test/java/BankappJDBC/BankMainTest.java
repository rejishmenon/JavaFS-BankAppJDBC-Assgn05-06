package BankappJDBC;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BankMainTest {

    private static int[] acctIds;

    private double balanceAcct0;
    private double balanceAcct1;

    @BeforeAll
    static void loadTestAccounts() throws SQLException {
        assumeTrue(BankMain.conn != null, "Database connection unavailable - skipping tests");
        try (Statement st = BankMain.conn.createStatement();
             ResultSet rs = st.executeQuery("select acct_id from bankacct order by acct_id")) {
            List<Integer> ids = new ArrayList<>();
            while (rs.next() && ids.size() < 2) {
                ids.add(rs.getInt(1));
            }
            assumeTrue(!ids.isEmpty(), "bankacct table has no accounts - skipping tests");
            acctIds = ids.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    @BeforeEach
    void snapshotBalances() throws SQLException {
        resetState();
        balanceAcct0 = rawBalance(acctIds[0]);
        balanceAcct1 = acctIds.length > 1 ? rawBalance(acctIds[1]) : 0;
    }

    @AfterEach
    void restoreBalances() throws SQLException {
        setBalance(acctIds[0], balanceAcct0);
        if (acctIds.length > 1) {
            setBalance(acctIds[1], balanceAcct1);
        }
    }

    // ---------- helpers ----------

    private void resetState() {
        BankMain.loop = true;
        BankMain.primeacct = 0;
        BankMain.name = null;
        BankMain.ps = null;
        BankMain.rset = null;
    }

    private void setInput(String input) {
        BankMain.scanner = new java.util.Scanner(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    private double rawBalance(int acct) throws SQLException {
        try (PreparedStatement ps = BankMain.conn.prepareStatement(
                "select balance from bankacct where acct_id=?")) {
            ps.setInt(1, acct);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Account " + acct + " not found");
                return rs.getDouble(1);
            }
        }
    }

    private void setBalance(int acct, double balance) throws SQLException {
        try (PreparedStatement ps = BankMain.conn.prepareStatement(
                "update bankacct set balance=? where acct_id=?")) {
            ps.setDouble(1, balance);
            ps.setInt(2, acct);
            ps.executeUpdate();
        }
    }

    // ---------- getBalance ----------

    @Test
    void getBalance_returnsActualBalance() {
        assertEquals(balanceAcct0, BankMain.getBalance(acctIds[0]), 0.001);
    }

    // ---------- checkAcct ----------

    @Test
    void checkAcct_validAccount_returnsRow() throws SQLException {
        ResultSet rs = BankMain.checkAcct(acctIds[0]);
        assertNotNull(rs);
        assertTrue(rs.next());
        assertEquals(acctIds[0], rs.getInt("acct_id"));
        rs.close();
    }

    @Test
    void checkAcct_invalidAccount_returnsEmptyResultSet() throws SQLException {
        ResultSet rs = BankMain.checkAcct(999999);
        assertNotNull(rs);
        assertFalse(rs.next());
        rs.close();
    }

    // ---------- deposit ----------

    @Test
    void deposit_positiveAmount_increasesBalance() {
        setInput("25.50\nY\n"); // amount, then continueYN prompt
        BankMain.deposit(acctIds[0]);
        assertEquals(balanceAcct0 + 25.50, BankMain.getBalance(acctIds[0]), 0.001);
    }

    @Test
    void deposit_zeroAmount_doesNotChangeBalance() {
        setInput("0\n");
        BankMain.deposit(acctIds[0]);
        assertEquals(balanceAcct0, BankMain.getBalance(acctIds[0]), 0.001);
    }

    @Test
    void deposit_negativeAmount_doesNotChangeBalance() {
        setInput("-10\n");
        BankMain.deposit(acctIds[0]);
        assertEquals(balanceAcct0, BankMain.getBalance(acctIds[0]), 0.001);
    }

    // ---------- withdraw ----------

    @Test
    void withdraw_sufficientFunds_decreasesBalance() {
        setInput("10.0\nY\n"); // amount, then continueYN prompt
        BankMain.withdraw(acctIds[0]);
        assertEquals(balanceAcct0 - 10.0, BankMain.getBalance(acctIds[0]), 0.001);
    }

    @Test
    void withdraw_insufficientFunds_doesNotChangeBalance() {
        assumeTrue(balanceAcct0 < 999999.0, "Test requires balance < 999999");
        setInput("999999.0\n");
        BankMain.withdraw(acctIds[0]);
        assertEquals(balanceAcct0, BankMain.getBalance(acctIds[0]), 0.001);
    }

    @Test
    void withdraw_zeroAmount_doesNotChangeBalance() {
        setInput("0\n");
        BankMain.withdraw(acctIds[0]);
        assertEquals(balanceAcct0, BankMain.getBalance(acctIds[0]), 0.001);
    }

    // ---------- transferFunds ----------

    @Test
    void transferFunds_validTransfer_movesAmount() {
        assumeTrue(acctIds.length > 1, "Need at least 2 accounts for transfer test");
        assumeTrue(balanceAcct0 >= 20.0, "Test requires source balance >= 20");
        setInput(acctIds[1] + "\n20.0\n"); // recipient acct, then amount
        BankMain.transferFunds(acctIds[0]);
        assertEquals(balanceAcct0 - 20.0, BankMain.getBalance(acctIds[0]), 0.001);
        assertEquals(balanceAcct1 + 20.0, BankMain.getBalance(acctIds[1]), 0.001);
    }

    @Test
    void transferFunds_amountExceedingBalance_doesNotChangeBalances() {
        assumeTrue(acctIds.length > 1, "Need at least 2 accounts for transfer test");
        assumeTrue(balanceAcct0 < 999999.0, "Test requires balance < 999999");
        setInput(acctIds[1] + "\n999999.0\n");
        BankMain.transferFunds(acctIds[0]);
        assertEquals(balanceAcct0, BankMain.getBalance(acctIds[0]), 0.001);
        assertEquals(balanceAcct1, BankMain.getBalance(acctIds[1]), 0.001);
    }

    // ---------- continueYN ----------

    @Test
    void continueYN_answerNo_setsLoopFalse() {
        setInput("n\n");
        BankMain.continueYN();
        assertFalse(BankMain.loop);
    }

    @Test
    void continueYN_answerYes_keepsLoopTrue() {
        setInput("y\n");
        BankMain.continueYN();
        assertTrue(BankMain.loop);
    }

    // ---------- displaybalance ----------

    @Test
    void displaybalance_doesNotThrow_andPromptsContinue() {
        setInput("Y\n");
        assertDoesNotThrow(() -> BankMain.displaybalance(acctIds[0]));
        assertTrue(BankMain.loop);
    }

    // ---------- main ----------

    @Test
    void main_validAccount_thenExitOption_quitsCleanly() {
        setInput(acctIds[0] + "\n5\n"); // acct id, then menu choice 5 = Exit
        BankMain.main(new String[]{});
        assertFalse(BankMain.loop);
    }

    @Test
    void main_invalidAccount_setsLoopFalse() {
        setInput("999999\n");
        BankMain.main(new String[]{});
        assertFalse(BankMain.loop);
    }
}
