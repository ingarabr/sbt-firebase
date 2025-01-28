function runApp() {
    console.log("Hi there")
    document.getElementById("app").innerHTML=Date();
}

document.addEventListener('DOMContentLoaded', function() {
    runApp();
}, false);