function change() {
    document.getElementById('sun').style.boxShadow='0px 0px 10px 10px hsl(41, 95%, 50%)';
}
function recover(){
    document.getElementById('sun').style.boxShadow='0px 0px 10px 2px black';
}
function change1(i) {
    var a=i.innerHTML.toLowerCase();
    document.getElementById(a).style.boxShadow='0px 0px 10px 10px white';
}
function recover1(i) {
    var a=i.innerHTML.toLowerCase();
    document.getElementById(a).style.boxShadow='0px 0px 10px 2px black';
}
function go(i) {
    var a=i.innerHTML.toLowerCase();
    if(a=='earth')a='earthS';
    if (a=='saturn')a='saturnS';
    if(document.getElementById(a).style.display=='none'){
        document.getElementById(a).style.display='';
    }
    else{
        document.getElementById(a).style.display='none';
    }
}


