<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Анаграми</title>
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
    function getAnagrams() {
        $("#results").html("Моля изчакайте...");
        var word = $("#word").val();
        $.post("anagrams/get", {word: word}, function(data) {
             var result = "";
             for (var i = 0; i < data.length; i++) {
                 result += data[i] + " ";
             }
             var tweetText = 'Анаграми на думата ' + word + ': ' + result + ". http://www.i-kratko.com/anagrams";
             result += '<br /><br /><a href="https://twitter.com/intent/tweet?text=' + tweetText + '">[туитни]</a>';
             $("#results").html(result);
        });
    }
    </script>
</head>
<body>
<div align="center">
    <h1>Анаграми</h1>
    <form>
    Дума: <input type="text" name="word" id="word" /><input type="submit" value="Покажи анаграмите" onclick="getAnagrams(); return false;"/></form> <br />
    <div id="results"></div>

    <hr style="margin-top: 20px;" />
    Някои анаграми:<br />
    анаграми: армагани<br />
    Доган: гадно годна<br />
    реклама: карамел кламера<br />
    Мария: армия</br />
    босилек: обелиск<br />
    Росита: асорти орисат ориста сирота осират аорист<br />
    Барселона: необрасла<br />
    Варна: врана равна навра<br />

    <div style="font-size: 0.8em; margin-top: 80px;">автор: <a href="http://bozho.net">Божидар Божанов</a></div>
</div>
</body>
</html>