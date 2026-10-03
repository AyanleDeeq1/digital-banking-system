import { useState } from "react"
import { useNavigate } from "react-router-dom"
import Header from '../components/Header.jsx'
import Footer from '../components/Footer.jsx'
import "../style/Login.css";
import Button from "../components/Button.jsx";


function Login({csrfToken, customer, setCustomer, logout}) {
    const navigate = useNavigate();
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')

    async function handleLogin(event) {
        event.preventDefault()

        const creds = {
            email,
            password
        }


        const req = await fetch('http://localhost:8080/api/customers/login', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-type': 'application/json',
                'X-XSRF-TOKEN': csrfToken
            },
            body: JSON.stringify(creds)
        })
        if (!req.ok) {
            return;
        }
        const response = await req.json()
        setCustomer(response)     
        console.log(response)
        navigate("/dashboard");
    }
    return (
        <div className="login-page">

                <Header logout={logout} page="login" customer={customer} />

                <main className="login-main">

                    <section className="login-card">

                        <div className="login-heading">
                            <p className="login-eyebrow">WELCOME BACK</p>
                            <h1>Log in to URBank</h1>
                            <p className="login-subtitle">
                                Access your accounts securely.
                            </p>
                        </div>

                        <form onSubmit={handleLogin}>

                            <div className="form-group">
                                <label htmlFor="email">Email address</label>

                                <input
                                    type="email"
                                    name="email"
                                    id="email"
                                    placeholder="janedoe@email.com"
                                    onChange={(event) => setEmail(event.target.value)}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="password">Password</label>

                                <input
                                    type="password"
                                    name="password"
                                    id="password"
                                    placeholder="Enter your password"
                                    onChange={(event) => setPassword(event.target.value)}
                                />
                            </div>

                                <Button type="submit">
                                        Log in
                                </Button>

                        </form>

                    </section>

                </main>

                <Footer />

        </div>
    );
}

export default Login