let currentStep = 1;


/*  CHANGE STEP */

function nextStep(step) {

    document.querySelectorAll(".register-step")
        .forEach(function (section) {
            section.classList.remove("active-step");
        });


    document.getElementById("step" + step)
        .classList.add("active-step");


    document.querySelectorAll(".step")
        .forEach(function (indicator) {
            indicator.classList.remove("active");
        });


    for (let i = 1; i <= step; i++) {

        document.getElementById("stepIndicator" + i)
            .classList.add("active");

    }


    currentStep = step;

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}


/*  SHOW / HIDE PASSWORD  */

document.addEventListener("DOMContentLoaded", function () {

    document.querySelectorAll(".password-toggle")
        .forEach(function (button) {

            button.addEventListener("click", function () {

                const targetId =
                    button.getAttribute("data-target");

                const input =
                    document.getElementById(targetId);


                if (input.type === "password") {

                    input.type = "text";

                    button.textContent = "Hide";

                } else {

                    input.type = "password";

                    button.textContent = "Show";

                }

            });

        });

});


/*  PROFILE IMAGE PREVIEW  */

function previewProfile(event) {

    const file = event.target.files[0];

    if (!file) {
        return;
    }


    const preview =
        document.getElementById("profilePreview");

    const placeholder =
        document.getElementById("profilePlaceholder");


    preview.src =
        URL.createObjectURL(file);

    preview.style.display = "block";

    placeholder.style.display = "none";
}


/*  INTEREST */

function toggleInterest(button) {

    button.classList.toggle("selected");

}


/*  CREATE ACCOUNT */

function createAccount() {

    alert("Account creation prototype complete!");

}

/*  CREATE OTHER GAME  */

function addCustomGame() {

    const input = document.getElementById("customGame");

    const gameName = input.value.trim();

    if (gameName === "") {
        return;
    }

    const gameList =
        document.getElementById("customGameList");


    const button =
        document.createElement("button");

    button.type = "button";

    button.className = "interest";

    button.textContent = "🎮 " + gameName;


    button.onclick = function () {

        toggleInterest(this);

    };


    gameList.appendChild(button);

    input.value = "";
}


/*  CREATE OTHER ACTIVITY */

function addCustomActivity() {

    const input =
        document.getElementById("customActivity");

    const activityName =
        input.value.trim();

    if (activityName === "") {
        return;
    }

    const activityList =
        document.getElementById("customActivityList");


    const button =
        document.createElement("button");

    button.type = "button";

    button.className = "interest";

    button.textContent = "💡 " + activityName;


    button.onclick = function () {

        toggleInterest(this);

    };


    activityList.appendChild(button);

    input.value = "";
}


/*  CREATE OTHER ENTERTAINMENT */

function addCustomEntertainment() {

    const input =
        document.getElementById("customEntertainment");

    const entertainmentName =
        input.value.trim();

    if (entertainmentName === "") {
        return;
    }

    const entertainmentList =
        document.getElementById(
            "customEntertainmentList"
        );


    const button =
        document.createElement("button");

    button.type = "button";

    button.className = "interest";

    button.textContent =
        "⭐ " + entertainmentName;


    button.onclick = function () {

        toggleInterest(this);

    };


    entertainmentList.appendChild(button);

    input.value = "";
}