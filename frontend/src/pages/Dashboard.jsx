import Header from '../components/Header.jsx'
import Footer from '../components/Footer.jsx'

function Dashboard({customer}) {
    if(!customer) {
        return <p>loading....</p>
    }
    return (
        <>
        <Header page="dashboard" customer={customer}/>
        <main>
            <h1>welcome </h1>
        </main>
        <Footer/>
        </>
    )
}

export default Dashboard