import { useEffect, useState } from "react";
import Header from "../components/Header.jsx";
import Footer from "../components/Footer.jsx";
import SideBar from "../components/SideBar.jsx";
import Button from "../components/Button.jsx";
import { useNavigate, useSearchParams } from "react-router-dom";
import "../style/Account.css";
import TransactionHistory from "../components/TransactionHistory.jsx";
import { formatBalance } from "../utils/formatBalance.js";

function Accounts({ customer , logout}) {
    const [accounts, setAccounts] = useState([]);
    const [accountsLoading, setAccountsLoading] = useState(true);
    const [accountsError, setAccountsError] = useState(null);
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const selectedAccount = accounts.find(account => String(account.id) === searchParams.get('account')) || accounts[0];

    useEffect(() => {
        async function getAccounts() {
            try {
                const request = await fetch(
                "http://localhost:8080/api/customers/accounts",
                {
                    method: "GET",
                    credentials: "include"
                }
            );

                if (!request.ok) {
                    const error = await request.json().catch(() => null);
                    setAccountsError(typeof error?.massage === "string" && request.status < 500
                        ? error.massage : "Unable to load accounts. Please try again.");
                    return;
                }
                const res = await request.json();
                if (!Array.isArray(res)) {
                    setAccountsError("Unable to read the account response. Please try again.");
                    return;
                }
                setAccounts(res);
            } catch {
                setAccountsError("Unable to load accounts. Check your connection and try again.");
            } finally {
                setAccountsLoading(false);
            }
        }

        getAccounts();
    }, []);

    if (!customer) {
        return accountsError ? <p role="alert">{accountsError}</p> : <p>Loading...</p>;
    }

    return (
        <div className="accounts-page">

            <Header logout={logout} page="accounts" customer={customer} />

            <div className="accounts-body">

                <SideBar logout={logout} />

                <main className="accounts-main">

                    <section className="accounts-page-heading">
                        <div>
                            <p className="section-eyebrow">
                                YOUR ACCOUNTS
                            </p>

                            <h1>Accounts</h1>

                            <p>
                                View and manage all your bank accounts.
                            </p>
                        </div>

                        <Button
                            variant="action"
                            onClick={() => navigate("/createAccount")}
                        >
                            + New Account
                        </Button>
                    </section>
                    {accountsLoading ? <p role="status">Loading accounts...</p>
                        : accountsError ? <p role="alert">{accountsError}</p>
                        : accounts.length === 0 ? <p>No accounts found.</p>
                        : <><section className="accounts-directory-heading"><div><h2>Your accounts <span>{accounts.length}</span></h2>
                            <p>Select an account to view its transaction history.</p></div></section>
                        <div className="accounts-table-container" tabIndex={0} role="region" aria-label="Your accounts table">
                        <table className="accounts-table">
                            <thead>
                                <tr>
                                    <th scope="col">Account Name</th>
                                    <th scope="col">Type</th>
                                    <th scope="col">Account Number</th>
                                    <th scope="col">Status</th>
                                    <th scope="col" className="account-balance">Balance (SEK)</th>
                                </tr>
                            </thead>

                            <tbody>
                                {accounts.map((account) => (
                                    <tr key={account.id} className={selectedAccount?.id === account.id ? 'selected-account' : ''}>
                                        <td className="account-name">
                                            <button type="button" className="account-select" aria-pressed={selectedAccount?.id === account.id}
                                                onClick={() => setSearchParams({ account: String(account.id) })}>{account.name}<span>View history →</span></button>
                                        </td>

                                        <td>
                                            <span className="account-type">
                                                {account.type}
                                            </span>
                                        </td>

                                        <td className="account-number">
                                            {account.accountNumber}
                                        </td>

                                        <td>
                                            <span
                                                className={`table-status ${account.status.toLowerCase()}`}
                                            >
                                                {account.status}
                                            </span>
                                        </td>
                                        <td className="account-balance">
                                            {formatBalance(account.balance)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    {selectedAccount && <TransactionHistory key={selectedAccount.id} accountId={selectedAccount.id} accountName={selectedAccount.name} />}</>}
                   

                </main>

            </div>

            <Footer />

        </div>
    );
}

export default Accounts;
