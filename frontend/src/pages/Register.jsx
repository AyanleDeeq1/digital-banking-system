import { useState } from "react"
import Header from '../components/Header.jsx';
import Footer from '../components/Footer.jsx'
import "../style/Register.css";
import Button from "../components/Button.jsx";

function Register({ csrfToken, customer}) {
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')

    async function handleSubmit(event) {
        event.preventDefault()

        const customer = {
            firstName,
            lastName,
            email,
            password
        }

        const request = await fetch('http://localhost:8080/api/customers', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken
            },
            credentials: "include",
            body: JSON.stringify(customer)
        })
        const response = await request.json()
        console.log(response)
    }
    return (
        <div className="register-page">

            <Header page="register" customer={customer} />

            <main className="register-main">

                <section className="register-card">

                    <div className="register-heading">
                        <p className="register-eyebrow">GET STARTED</p>

                        <h1>Create your URBank account</h1>

                        <p className="register-subtitle">
                            Simple, secure banking starts here.
                        </p>
                    </div>

                    <form onSubmit={handleSubmit}>

                        <div className="register-name-row">

                            <div className="form-group">
                                <label htmlFor="firstName">First name</label>
                                <input
                                    type="text"
                                    name="firstName"
                                    id="firstName"
                                    placeholder="Jane"
                                    onChange={(event) => setFirstName(event.target.value)}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="lastName">Last name</label>
                                <input
                                    type="text"
                                    name="lastName"
                                    id="lastName"
                                    placeholder="Doe"
                                    onChange={(event) => setLastName(event.target.value)}
                                />
                            </div>

                        </div>

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
                                placeholder="Create a password"
                                onChange={(event) => setPassword(event.target.value)}
                            />
                        </div>

                        <Button type="submit">
                                Create account
                        </Button>

                    </form>

                </section>

            </main>

            <Footer />

        </div>
    );
}
export default Register