import { Link } from "react-router-dom";
import Header from '../components/Header.jsx'
import Footer from '../components/Footer.jsx'

function Home({customer}) {
  return (
    <>
      <Header page="home" customer={customer}/>
      <main>
           <h1>Welcome to URBank</h1>

           <p>
              Save, secure, and easily handle your money with ease.
            </p>
      </main>
      <Footer/>
    </>
  )
}

export default Home
