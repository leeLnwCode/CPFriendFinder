document.addEventListener("DOMContentLoaded", function () {

    const password =
        document.getElementById("password");

    const showPassword =
        document.getElementById("showPassword");


    showPassword.addEventListener("click", function () {

        if (password.type === "password") {

            password.type = "text";

            showPassword.textContent = "Hide";

        } else {

            password.type = "password";

            showPassword.textContent = "Show";

        }

    });

});