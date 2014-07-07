<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Рими</title>
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
    function getRhymes() {
        $("#results").html("Моля изчакайте...");
        var ending = $("#ending").val();
        var syllables = $("#syllables").val();
        $.post("rhymes/find", {ending: ending, syllables: syllables}, function(data) {
             var result = "";
             for (var i = 0; i < data.length; i++) {
                 result += data[i] + " ";
             }
             $("#results").html(result);
        });
    }

    function getSimilarEndings() {
        $("#similarEndings").html("Моля изчакайте...");
        var ending = $("#ending").val();
        $.post("rhymes/similarEndings", {ending: ending}, function(data) {
             var result = "";
             for (var i = 0; i < data.length; i++) {
                 result += data[i] + " ";
             }
             $("#similarEndings").html(result + "<hr /><br />");
        });
    }
    </script>
</head>
<body>
<div align="center">
    <h1>Рими</h1>
    <form>
    Окончание: <input type="text" name="ending" id="ending" /><input type="button" value="Покажи подобни окончания" onclick="getSimilarEndings()"/><br />
    Ограничи по срички:<input type="checkbox" onclick="$('#syllables').attr('disabled',!this.checked);"/><input type="text" size="4" name="syllables" id="syllables" value="0" disabled="true" />срички<br />
    <input type="submit" value="Покажи римите" onclick="getRhymes(); return false;"/></form> <br />
    <div id="similarEndings"></div>
    <div id="results"></div>

    <div style="font-size: 0.8em; margin-top: 80px;">автор: <a href="http://bozho.net">Божидар Божанов</a></div>
</div>
</body>
</html>