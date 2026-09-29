import { useEffect, useState } from "react";
import AccountCard from "../components/AccountCard.jsx";
import Header from "../components/Header.jsx";
import Footer from "../components/Footer.jsx";
import SideBar from "../components/SideBar.jsx";
import Button from "../components/Button.jsx";
import { useNavigate } from "react-router-dom";
import "../style/Account.css";
import AccountsRow from "../components/AccountsRow.jsx";

function Accounts({ customer }) {
    const [accounts, setAccounts] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        async function getAccounts() {
            const request = await fetch(
                "http://localhost:8080/api/customers/accounts",
                {
                    method: "GET",
                    credentials: "include"
                }
            );

            if (!request.ok) {
                setAccounts([]);
            } else {
                const res = await request.json();
                console.log(res);
                setAccounts(res);
            }
        }

        getAccounts();
    }, []);

    if (!customer) {
        return <p>Loading...</p>;
    }

    return (
        <div className="accounts-page">

            <Header page="accounts" customer={customer} />

            <div className="accounts-body">

                <SideBar />

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
                    <div className="accounts-table-container">
                        <table className="accounts-table">
                            <thead>
                                <tr>
                                    <th>Account Name</th>
                                    <th>Type</th>
                                    <th>Account Number</th>
                                    <th>Status</th>
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
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                   

                </main>

            </div>

            <Footer />

        </div>
    );
}

export default Accounts;