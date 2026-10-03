import { useEffect, useState } from "react";
import AccountCard from "../components/AccountCard.jsx";
import Header from "../components/Header.jsx";
import Footer from "../components/Footer.jsx";
import SideBar from "../components/SideBar.jsx";
import Button from "../components/Button.jsx";
import { useNavigate } from "react-router-dom";
import "../style/Account.css";
import AccountsRow from "../components/AccountsRow.jsx";
import { formatBalance } from "../utils/formatBalance.js";

function Accounts({ customer , logout}) {
    const [accounts, setAccounts] = useState([]);
    const [accountsLoading, setAccountsLoading] = useState(true);
    const [accountsError, setAccountsError] = useState(null);
    const navigate = useNavigate();

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
                        : <div className="accounts-table-container">
                        <table className="accounts-table">
                            <thead>
                                <tr>
                                    <th>Account Name</th>
                                    <th>Type</th>
                                    <th>Account Number</th>
                                    <th>Status</th>
                                    <th className="account-balance">Balance (SEK)</th>
                                </tr>
                            </thead>

                            <tbody>
                                {accounts.map((account) => (
                                    <tr key={account.id}>
                                        <td className="account-name">
                                            {account.name}
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
                    </div>}
                   

                </main>

            </div>

            <Footer />

        </div>
    );
}

export default Accounts;
