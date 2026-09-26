import { useState } from "react"
import Header from '../components/Header.jsx';
import Footer from '../components/Footer.jsx'

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
        <>
              <Header page="register" customer={customer}/>
            <main>
                <form onSubmit={handleSubmit}>
                    <label htmlFor="firstName">Enter firstname</label>
                    <input type="text"
                    name="firstName"
                    id="firstName" 
                    onChange={(event) => setFirstName(event.target.value)}/> <br />
                    
                    <label htmlFor="lastName">Enter lastname</label>
                    <input 
                        type="text" 
                        name="lastName" 
                        id="lastName"
                        onChange={(event) => setLastName(event.target.value)}/> <br />

                    
                    <label htmlFor="email">Enter Email</label>
                    <input 
                        type="email" 
                        name="email" 
                        id="email" 
                        onChange={(event) => setEmail(event.target.value)} /> <br />

                    <label htmlFor="password">Enter password</label>
                    <input 
                        type="password" 
                        name="password" 
                        id="password" onChange={(event) => setPassword(event.target.value)} />
                    <button type="submit">Register</button>
                </form>
            </main>
            <Footer/>
        </>
    )
}
export default Register