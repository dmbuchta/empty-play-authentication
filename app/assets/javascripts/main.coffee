window.createAlert = (type, msg, selector, dismissable) ->
  $("<div class='alert alert-#{type}' role='alert'>" +
    "<a class='#{if dismissable then "close" else "hidden-xs-up"}' data-dismiss='alert'>×</a>" +
    "<span>#{msg}</span>" +
  "</div>").appendTo(selector)