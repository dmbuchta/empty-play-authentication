window.createAlert = (type, msg, selector, dismissable) ->
  $("<div class='alert alert-#{type}' role='alert'>" +
      "<a class='#{if dismissable then "close" else "hidden-xs-up"}' data-dismiss='alert'>×</a>" +
      "<span>#{msg}</span>" +
      "</div>").appendTo(selector)

$ ->
  # This was added because occasionally the validator plugin would not bind to the submit event fast enough
  # causing my submit handlers to be bound before the validator.
  $("form[data-toggle='validator']").validator();