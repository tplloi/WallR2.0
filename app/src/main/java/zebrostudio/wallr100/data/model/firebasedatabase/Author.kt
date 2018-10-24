package zebrostudio.wallr100.data.model.firebasedatabase

import com.google.gson.annotations.SerializedName

data class Author(
  @SerializedName("name") val name: String,
  @SerializedName("profileImageUrl") val profileImageLink: String
)