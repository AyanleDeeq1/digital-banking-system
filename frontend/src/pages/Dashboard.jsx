import Header from '../components/Header.jsx'
import Footer from '../components/Footer.jsx'
import SideBar from '../components/SideBar.jsx'
import "../style/Dashboard.css"
import Button from "../components/Button.jsx";
import AccountCard from "../components/AccountCard.jsx";
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AccountSummary from '../components/AccountSummary.jsx';

function Dashboard({customer}) {
    const [accounts, setAccounts] = useState([])
    const navigate = useNavigate()
    const mainAccount = accounts[0]

    useEffect(() => {
        async function getAccounts() {
            const  request = await fetch("http://localhost:8080/api/customers/accounts", {
                method: 'GET',
                credentials: 'include'
            })
            if(!request.ok) {
                setAccounts([])
            } else {
                const res = await request.json()
                console.log(res)
                setAccounts(res)
            }
        }
        getAccounts()
    }, [])

    if(!customer) {
        return <p>loading....</p>
    }
    
    return (
        <div className="dashboard-page">

            <Header page="dashboard" customer={customer} />

            <div className="dashboard-body">
                <SideBar />

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
                        <div className="accounts-grid">
                            {mainAccount && <AccountCard  account={mainAccount}/>}
                            <AccountSummary  accounts={accounts}/>

                
                        </div>
                        <section className="quick-actions">
                            <h2>Quick Actions</h2>
                        </section>
                    </section>
                    
                </main>

            </div>

            <Footer />

        </div>
    )
}

export default Dashboard