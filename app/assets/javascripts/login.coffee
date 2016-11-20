$ ->
  $("#sign-up-modal-yes").click (e) ->
    $("#sign-up-form").submit();

  $("#sign-up-form").submit (e) ->
    $("#sign-up-modal-alerts").empty();
    # if the form passed validation, we then want to send it via ajax
    if not e.isDefaultPrevented()
      e.preventDefault();
      $.ajax $(this).attr("action"),
        method: $(this).attr "method"
        data: $(this).serialize()
        dataType: "json"
        success: (response) ->
          if response.success
            return window.location = response.url

          if response.message
            createAlert "danger", response.message, "#sign-up-modal-alerts"
          else if response.formErrors
            createAlert "danger", "Please fix the following errors before continuing", "#sign-up-modal-alerts"
            for field,errors of response.formErrors
              $("##{field}").closest(".form-group")
              .removeClass "has-success"
              .addClass "has-danger"
          else
            createAlert "danger", "There appears to be something wrong with your form", "#sign-up-modal-alerts"
        error: (response) ->
          createAlert "danger", "An error occurred creating new account.", "#sign-up-modal-alerts"

  $('#sign-up-modal').on 'hidden.bs.modal', (e) ->
    $("#sign-up-modal-alerts").empty()
    $("#sign-up-form input").val ""
    $("#sign-up-form .form-group").removeClass "has-success has-danger"