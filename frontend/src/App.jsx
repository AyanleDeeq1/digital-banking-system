import { Routes, Route } from 'react-router-dom'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import { useEffect, useState } from 'react'

function App() {

  const [csrfToken, setCsrfToken] = useState(null);
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
    getCsrfToken()
  }, [])
  return (
    <Routes>
      <Route path='/' element={<Home />} />
      <Route path='/login' element={<Login csrfToken={csrfToken}/>} />
      <Route path='/register' element={<Register csrfToken={csrfToken} />} />
    </Routes>
  )
}

export default App
