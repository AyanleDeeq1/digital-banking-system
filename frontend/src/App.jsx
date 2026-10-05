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
  const csrfRequest = useRef(null);
  const [csrfError, setCsrfError] = useState('');
  const [customer, setCustomer] = useState(null)
  const [customerStatus, setCustomerStatus] = useState("loading");
  useEffect(() => {
    let cancelled = false;
    async function initialize() {
      try {
      const csrf = await fetchCsrf();
      if (cancelled) return;
      const request = await fetch("/api/customers/me", {
        method: "GET",
        credentials: "include"
      });
      if (cancelled) return;

      if (request.ok) {
        const res = await request.json()
        if (cancelled) return;
        setCustomer(res)
        setCustomerStatus("ready");
      } else {
        setCustomer(null)
        setCustomerStatus(request.status === 401 || request.status === 403 ? "unauthenticated" : "error");
      }
      setCsrfToken(csrf.token);
      } catch {
        if (cancelled) return;
        setCustomerStatus("error");
        setCsrfError('Unable to initialize your security session. Check your connection and try again.');
      }
    }

    initialize();
    return () => { cancelled = true; };
  }, [])
  
  const navigate = useNavigate();
  const logoutBusy = useRef(false);
  const [logoutPending, setLogoutPending] = useState(false);
  const [logoutError, setLogoutError] = useState('');

  function fetchCsrf() {
    // Share an in-flight load, including React StrictMode's initial effect replay.
    if (!csrfRequest.current) {
      csrfRequest.current = (async () => {
        const response = await fetch('/api/customers/csrf', { credentials: 'include', cache: 'no-store' });
        if (!response.ok) throw new Error('Unable to verify your security session. Please try again.');
        const token = await response.json();
        if (typeof token.token !== 'string' || !token.token || token.headerName !== 'X-XSRF-TOKEN') {
          throw new Error('Unable to verify your security session. Please try again.');
        }
        return token;
      })().finally(() => { csrfRequest.current = null; });
    }
    return csrfRequest.current;
  }

  function handleLogin(customer) {
    setCustomer(customer);
    setCustomerStatus('ready');
  }

  async function handleLogout() {
    if (logoutBusy.current) return;
    logoutBusy.current = true;
    setLogoutPending(true); setLogoutError('');
    try {
      const csrf = await fetchCsrf();
      const response = await fetch('/api/customers/logout', {
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
    <>
    {csrfError && <p role="alert">{csrfError}</p>}
    <Routes>
      <Route path='/' element={<Home logout={logout}  customer={customer}/>} />
      <Route path='/login' element={<Login logout={logout} csrfToken={csrfToken} customer={customer} setCustomer={handleLogin}/>} />
      <Route path='/register' element={<Register logout={logout} csrfToken={csrfToken} customer={customer} />} />
      <Route path='/dashboard' element={<Dashboard logout={logout} customer={customer} csrfToken={csrfToken}/>} />
      <Route path='/createAccount' element={<CreateAccount logout={logout} csrfToken={csrfToken} customer={customer} />} />
      <Route path='/accounts' element={<Accounts logout={logout} customer={customer} />} />
      <Route path='/my-card' element={<MyCard logout={logout} customer={customer} />} />
      <Route path='/transfer' element={<Transfer logout={logout} customer={customer} customerStatus={customerStatus} />} />
      <Route path='/deposit-withdraw' element={<DepositWithdraw logout={logout} customer={customer} customerStatus={customerStatus} />} />
      <Route path='/profile' element={<Profile logout={logout} customer={customer} customerStatus={customerStatus} />} />
    </Routes>
    </>
  )
}

export default App
