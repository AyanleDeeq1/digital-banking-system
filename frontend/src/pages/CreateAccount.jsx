import "../style/CreateAccount.css";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import Header from "../components/Header.jsx";
import Footer from "../components/Footer.jsx";
import SideBar from "../components/SideBar.jsx";
import Button from "../components/Button.jsx";

function CreateAccount({csrfToken, customer, logout}) {
    const [success, setSuccess] = useState(false)
    const [accountName, setAccountName] = useState(null)
    const [accountType, setAccountType] = useState("CHECKING");
    const navigate = useNavigate();

    async function  createNewAccount(event) {
            event.preventDefault();
        if (!csrfToken) return;
        const request = await fetch("/api/customers/createAccount", {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                "X-XSRF-TOKEN": csrfToken

            },
            body: JSON.stringify({
                name: accountName,
                accountType: accountType
            } )
        });
        if (request.ok) {
            setSuccess(true)
            setTimeout(() => {
                navigate("/dashboard")
            }, 1000)
        }
    }
    if (success) {
    return (
            <div className="create-success-page">
                <div className="create-success-card">

                    <div className="success-circle">
                        <span className="success-tick">✓</span>
                    </div>

                    <h2>Account Created!</h2>

                    <p>
                        Your new account has been created successfully.
                    </p>

                    <span className="redirect-message">
                        Redirecting to your dashboard...
                    </span>

                </div>
            </div>
        );
    }
    return (
        <div className="create-account-page">
            <Header logout={logout} page="dashboard" customer={customer} />

            <div className="create-account-body">
                <SideBar logout={logout} />

                <main className="create-account-main">

                    <section className="create-account-heading">
                        <p className="section-eyebrow">CREATE ACCOUNT</p>
                        <h1>Open a new account</h1>
                        <p>
                            Add another account to your banking profile.
                        </p>
                    </section>

                    <form
                        className="create-account-form"
                        onSubmit={createNewAccount}
                    >
                        <div className="form-heading">
                            <h2>Account details</h2>
                            <p>Choose a name and type for your new account.</p>
                        </div>

                        <div className="form-group">
                            <label htmlFor="accountName">
                                Account name
                            </label>

                            <input
                                type="text"
                                id="accountName"
                                placeholder="e.g. Holiday Savings"
                                value={accountName}
                                onChange={(event) =>
                                    setAccountName(event.target.value)
                                }
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="accountType">
                                Account type
                            </label>

                            <select
                                id="accountType"
                                value={accountType}
                                onChange={(event) =>
                                    setAccountType(event.target.value)
                                }
                            >
                                <option value="CHECKING">Checking</option>
                                <option value="SAVINGS">Savings</option>
                            </select>
                        </div>

                        <div className="create-account-actions">
                            <Button
                                variant="secondary"
                                type="button"
                                onClick={() => navigate("/dashboard")}
                            >
                                Cancel
                            </Button>

                            <Button
                                variant="action"
                                type="submit"
                                disabled={!csrfToken}
                            >
                                Create Account
                            </Button>
                        </div>
                    </form>

                </main>
        </div>

        <Footer />
    </div>
    );

    

}
export default CreateAccount
