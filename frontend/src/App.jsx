import { Routes, Route } from 'react-router-dom'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Dashboard from './pages/Dashboard.jsx'
import { useEffect, useState } from 'react'
import CreateAccount from './pages/CreateAccount.jsx'
import Accounts from "./pages/Accounts.jsx";
import MyCard from "./pages/MyCard.jsx";
import Profile from "./pages/Profile.jsx";

function App() {

  const [csrfToken, setCsrfToken] = useState(null);
  const [customer, setCustomer] = useState(null)
  const [customerStatus, setCustomerStatus] = useState("loading");
  useEffect(() => {
    async function getCsrfToken() {
      const request = await fetch("http://localhost:8080/api/customers/csrf", {
        method: "GET",
        credentials: "include"
      });
      const data = await request.json();
      console.log(data);
      setCsrfToken(data.token)
      
    }

    async function getCurrentCustomer() {
      try {
      const request = await fetch("http://localhost:8080/api/customers/me", {
        method: "GET",
        credentials: "include"
      });

      if (request.ok) {
        const res = await request.json()
        setCustomer(res)
        setCustomerStatus("ready");
      } else {
        setCustomer(null)
        setCustomerStatus(request.status === 401 || request.status === 403 ? "unauthenticated" : "error");
      }
      } catch {
        setCustomerStatus("error");
      }
    }

    getCsrfToken();
    getCurrentCustomer();
  }, [])
  
  return (
    <Routes>
      <Route path='/' element={<Home  customer={customer}/>} />
      <Route path='/login' element={<Login csrfToken={csrfToken} customer={customer} setCustomer={setCustomer}/>} />
      <Route path='/register' element={<Register csrfToken={csrfToken} customer={customer} />} />
      <Route path='/dashboard' element={<Dashboard customer={customer} />} />
      <Route path='/createAccount' element={<CreateAccount csrfToken={csrfToken} customer={customer} />} />
      <Route path='/accounts' element={<Accounts customer={customer} />} />
      <Route path='/my-card' element={<MyCard customer={customer} />} />
      <Route path='/profile' element={<Profile customer={customer} customerStatus={customerStatus} />} />
    </Routes>
  )
}

export default App
