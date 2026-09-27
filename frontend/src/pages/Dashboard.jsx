import Header from '../components/Header.jsx'
import Footer from '../components/Footer.jsx'
import SideBar from '../components/SideBar.jsx'
import "../style/Dashboard.css"
import Button from "../components/Button.jsx";
import AccountCard from "../components/AccountCard.jsx";

function Dashboard({customer}) {
    if(!customer) {
        return <p>loading....</p>
    }

   const accounts = [
    {
        id: 1,
        name: "Main Account",
        accountNumber: "123456789",
        accountType: "CHECKING",
        status: "ACTIVE"
    },
    {
        id: 2,
        name: "Savings Account",
        accountNumber: "987654321",
        accountType: "SAVINGS",
        status: "FROZEN"
    }
];
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

                            <Button variant='action'>
                                 + New Account
                            </Button>
                        </div>
                        <div className="accounts-grid">
                            {accounts.map((account) => (
                                <AccountCard
                                    key={account.id}
                                    account={account}
                                />
                            ))}
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