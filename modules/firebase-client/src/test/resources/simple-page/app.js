function runApp() {
    console.log("Hi")
    document.getElementById("app").innerHTML=Date();
}

document.addEventListener('DOMContentLoaded', function() {
    runApp();
}, false);