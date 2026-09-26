import { useState } from "react"
import { useNavigate } from "react-router-dom"
import Header from '../components/Header.jsx'
import Footer from '../components/Footer.jsx'


function Login({csrfToken, customer, setCustomer}) {
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
        <>
              <Header page="login" customer={customer}/>
            <main>
                <form onSubmit={handleLogin}>
                    <label htmlFor="email">Enter your email</label>
                    <input 
                        type="email" 
                        name="email" 
                        id="email" 
                        placeholder="janedoe@email.com" 
                        onChange={(event) => setEmail(event.target.value)} /> <br />

                    <label htmlFor="password">Enter passowrd</label>
                    <input 
                        type="password" 
                        name="password" 
                        id="password" 
                        onChange={(event) => setPassword(event.target.value)}/>
                    <button type="submit">Login</button>
                </form>
            </main>
            <Footer/>
        </>
        
    )
}

export default Login