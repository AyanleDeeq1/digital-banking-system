function Register() {
    return (
        <div>
            <form action="">
                <label htmlFor="firstName">Enter firstname</label>
                <input type="text" name="firstName" id="firstName" /> <br />
                 <label htmlFor="lastName">Enter lastname</label>
                <input type="text" name="lastName"  id="lastName"/> <br />
                 <label htmlFor="email">Enter Email</label>
                <input type="email" name="email" id="email"/> <br />
                <label htmlFor="password">Enter password</label>
                <input type="password" name="password" id="password" />
            </form>
        </div>
    )
}
export default Register