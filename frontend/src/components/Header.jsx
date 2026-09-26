import { Link } from "react-router-dom";

function Header({customer, page}) {
  
    return (
        <header>
            <h1>UrBank</h1>

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
        </header>
    )
}

export default Header