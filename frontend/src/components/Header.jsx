import { Link } from "react-router-dom";
import logo from '../assets/logo.png'
import '../style/Header.css'

function Header({customer, page}) {
  
    return (
        <header>
            <div className="header-logo">
                <Link to="/">
                    <img src={logo} alt="UrBank" />
                </Link>
            </div>

            <nav className="header-nav">

                    {!customer && page === "home" && (
                        <>
                        <Link to="/login">Login</Link>
                        <Link to="/register">Register</Link>
                        </>
                    ) }

                    {!customer && page==="register" &&
                    
                        <>
                            <Link to="/">Home</Link>
                            <Link to="/login">Login</Link>
                        </>
                    
                    }

                    {!customer && page==="login" &&
                        <>
                        <Link to="/">Home</Link>
                        <Link to="/register">Register</Link>
                        </>
                    }

                    {customer && (
                        <>
                        {page !== "home" && 
                                <Link to="/">Home</Link>
                            }
                        {page !== "dashboard" && 
                            <Link to="/dashboard">Dashboard</Link>
                        }
                        {page !== "profile" && 
                            <Link to="/profile">Profile</Link>
                        }
                        <button>Logout</button>
                        </>
                    )}
            </nav>
        </header>
    )
}

export default Header