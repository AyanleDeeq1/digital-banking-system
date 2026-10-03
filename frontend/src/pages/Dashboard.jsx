import Header from '../components/Header.jsx'
import Footer from '../components/Footer.jsx'
import SideBar from '../components/SideBar.jsx'
import "../style/Dashboard.css"
import Button from "../components/Button.jsx";
import AccountCard from "../components/AccountCard.jsx";
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AccountSummary from '../components/AccountSummary.jsx';
import deposit from '../assets/deposit.png'
import transfer from '../assets/transfer.png'
import TransactionHistory from '../components/TransactionHistory.jsx';


function Dashboard({customer, logout}) {
    const [accounts, setAccounts] = useState([])
    const [accountsLoading, setAccountsLoading] = useState(true)
    const [accountsError, setAccountsError] = useState(null)
    const navigate = useNavigate()
    const mainAccount = [...accounts].sort((a, b) => a.id - b.id)[0]

    useEffect(() => {
        async function getAccounts() {
            try {
                const request = await fetch("http://localhost:8080/api/customers/accounts", {
                    method: 'GET',
                    credentials: 'include'
                })
                if (!request.ok) {
                    const error = await request.json().catch(() => null)
                    setAccountsError(typeof error?.massage === "string" && request.status < 500
                        ? error.massage : "Unable to load accounts. Please try again.")
                    return
                }
                const res = await request.json()
                if (!Array.isArray(res)) {
                    setAccountsError("Unable to read the account response. Please try again.")
                    return
                }
                setAccounts(res)
            } catch {
                setAccountsError("Unable to load accounts. Check your connection and try again.")
            } finally {
                setAccountsLoading(false)
            }
        }
        getAccounts()
    }, [])

    if(!customer) {
        return accountsError ? <p role="alert">{accountsError}</p> : <p>loading....</p>
    }
    
    return (
        <div className="dashboard-page">

            <Header logout={logout} page="dashboard" customer={customer} />

            <div className="dashboard-body">
                <SideBar logout={logout}/>

                <main className="dashboard-main">
                     <section className="dashboard-heading">
                        <h1>Welcome back, {customer.firstName}</h1>
                        <p>Manage your accounts and banking from one place.</p>
                    </section>

                    <section className="accounts-section">
                        <div className="accounts-heading">
                            <div>
                                <p className="section-eyebrow">YOUR ACCOUNTS</p>
                                <h2>Accounts</h2>
                            </div>

                            <Button variant='action' onClick={() => navigate("/createAccount")}>
                                 + New Account
                            </Button>
                        </div>
                        {accountsLoading ? <p role="status">Loading accounts...</p>
                            : accountsError ? <p role="alert">{accountsError}</p>
                            : accounts.length === 0 ? <p>No accounts found.</p>
                            : <div className="accounts-grid">
                            {mainAccount && <div className="dashboard-account"><AccountCard account={mainAccount}/>
                                <button className="dashboard-account-link" type="button" onClick={() => navigate(`/accounts?account=${mainAccount.id}`)}>View transaction history →</button></div>}
                            <AccountSummary  accounts={accounts}/>

                
                        </div>}
                        <section className="quick-actions">
                            <h2>Quick Actions</h2>

                            <div className="quick-actions-grid">
                                <button className="quick-action-card" type="button" onClick={() => navigate("/transfer")}>
                                    <div className="quick-action-icon">
                                        <img src={transfer} alt="transfer" />
                                    </div>

                                    <div className="quick-action-content">
                                        <h3>Transfer Money</h3>
                                        <p>Send money to another account</p>
                                    </div>
                                </button>

                                <button className="quick-action-card" type="button" onClick={() => navigate("/deposit-withdraw")}>
                                    <div className="quick-action-icon">
                                        <img src= {deposit} alt="deposit" />
                                    </div>

                                    <div className="quick-action-content">
                                        <h3>Deposit / Withdraw</h3>
                                        <p> Use the URBank ATM</p>
                                    </div>
                                </button>

                            </div>
                        </section>
                    </section>
                    <TransactionHistory recent />
                    
                </main>

            </div>

            <Footer />

        </div>
    )
}

export default Dashboard
