<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Пароними</title>
    <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.6/jquery.min.js"></script>
    <%@ include file="analytics.jsp" %>
    <script type="text/javascript">
    $(document).ready(function() {
        $.ajaxSetup({
            // Disable caching of AJAX responses
            cache: false,
            timeout: 60000,
            complete: function(jqxhr, textStatus) {
                if (jqxhr.status != 200) {
                    $("#results").html("");
                }
            }
        });
    });
    function getParonyms() {
        $("#results").html("Моля изчакайте...");
        var word = $("#word").val();
        $.post("paronyms/find", {word: word}, function(data) {
             var result = "";
             for (var i = 0; i < data.length; i++) {
                 result += data[i] + " ";
             }
             $("#results").html(result);
        });
    }
    </script>
</head>
<body>
<div align="center">
    <h1>Пароними</h1>
    <form>
    Дума: <input type="text" name="word" id="word" /><input type="submit" value="Покажи паронимите" onclick="getParonyms(); return false;"/></form> <br />
    <div id="results"></div>

    <hr style="margin-top: 20px;" />
    Паронимите са думи със сходно звучене. Резултатите не винаги ще бъдат истински пароними, но алгоритъмът е далеч от перфектен и не знае кое наистина звучи подобно и кое не. Ето няколко примера за пароними:<br />
    магистрати: магистрали<br />
    нотариален: натурален<br />
    батерия: бактерия<br />
    <div style="font-size: 0.8em; margin-top: 80px;">автор: <a href="http://bozho.net">Божидар Божанов</a></div>
</div>
</body>
</html>