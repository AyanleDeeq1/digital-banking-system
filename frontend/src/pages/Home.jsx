import { Link } from "react-router-dom";
import Header from '../components/Header.jsx'
import Footer from '../components/Footer.jsx'
import BankCard from '../components/BankCard.jsx'
import '../style/Home.css'

function Home({customer, logout}) {
  return (
        <div className="home-page">

            <Header logout={logout} page="home" customer={customer} />

            <main className="home-main">

                <section className="home-intro">
                          <p className="home-eyebrow">SMARTER DIGITAL BANKING</p>

                          <h1>
                              Your money.
                              <br />
                              <span>Your way.</span>
                          </h1>

                          <p className="home-description">
                              Simple, secure banking designed to keep you in control
                              of your money — wherever life takes you.
                          </p>
                </section>

                <BankCard
                    firstName="John"
                    lastName="Doe"
                    lastFour="1234"
                    validDate="02/30"
                    cvc2="123"
                    type="Debit"
                />

            </main>

            <Footer />

        </div>
    );
}

export default Home
