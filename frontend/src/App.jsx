import { Routes, Route, useNavigate } from 'react-router-dom'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Dashboard from './pages/Dashboard.jsx'
import { useEffect, useRef, useState } from 'react'
import CreateAccount from './pages/CreateAccount.jsx'
import Accounts from "./pages/Accounts.jsx";
import MyCard from "./pages/MyCard.jsx";
import Transfer from './pages/Transfer.jsx';
import DepositWithdraw from './pages/DepositWithdraw.jsx';
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
  
  const navigate = useNavigate();
  const logoutBusy = useRef(false);
  const [logoutPending, setLogoutPending] = useState(false);
  const [logoutError, setLogoutError] = useState('');

  async function fetchCsrf() {
    const response = await fetch('http://localhost:8080/api/customers/csrf', { credentials: 'include' });
    if (!response.ok) throw new Error('Unable to verify your security session. Please reload and try again.');
    const token = await response.json();
    if (!token.token || !token.headerName) throw new Error('Unable to verify your security session. Please reload and try again.');
    return token;
  }

  async function handleLogout() {
    if (logoutBusy.current) return;
    logoutBusy.current = true;
    setLogoutPending(true); setLogoutError('');
    try {
      const csrf = await fetchCsrf();
      const response = await fetch('http://localhost:8080/api/customers/logout', {
        method: 'POST', credentials: 'include',
        headers: { [csrf.headerName]: csrf.token },
      });
      if (!response.ok) {
        throw new Error(response.status === 403
          ? 'Logout could not verify your security session. Please retry.'
          : 'Unable to log out. Please try again.');
      }
      setCustomer(null); setCustomerStatus('unauthenticated'); setCsrfToken(null);
      navigate('/', { replace: true });
      try { setCsrfToken((await fetchCsrf()).token); }
      catch { setLogoutError('You are signed out. Reload the page before signing in again.'); }
    } catch (failure) {
      setLogoutError(failure instanceof TypeError
        ? 'Unable to confirm logout. Check your connection and try again.' : failure.message);
    } finally {
      logoutBusy.current = false; setLogoutPending(false);
    }
  }
  const logout = { pending: logoutPending, error: logoutError, submit: handleLogout };

  return (
    <Routes>
      <Route path='/' element={<Home logout={logout}  customer={customer}/>} />
      <Route path='/login' element={<Login logout={logout} csrfToken={csrfToken} customer={customer} setCustomer={setCustomer}/>} />
      <Route path='/register' element={<Register logout={logout} csrfToken={csrfToken} customer={customer} />} />
      <Route path='/dashboard' element={<Dashboard logout={logout} customer={customer} csrfToken={csrfToken}/>} />
      <Route path='/createAccount' element={<CreateAccount logout={logout} csrfToken={csrfToken} customer={customer} />} />
      <Route path='/accounts' element={<Accounts logout={logout} customer={customer} />} />
      <Route path='/my-card' element={<MyCard logout={logout} customer={customer} />} />
      <Route path='/transfer' element={<Transfer logout={logout} customer={customer} customerStatus={customerStatus} />} />
      <Route path='/deposit-withdraw' element={<DepositWithdraw logout={logout} customer={customer} customerStatus={customerStatus} />} />
      <Route path='/profile' element={<Profile logout={logout} customer={customer} customerStatus={customerStatus} />} />
    </Routes>
  )
}

export default App
