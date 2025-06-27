	var pages = [];
	var currentPage = 0;
	document.addEventListener('DOMContentLoaded', function() {
		pages = document.getElementsByClassName("page");
		for ( var i = 0; i < pages.length; i++) {
			pages[i].style.display = "none";
		}
	
		document.getElementById('kb-footer-label').textContent = (currentPage + 1)
				+ "/" + pages.length
	
		pages[currentPage].style.display = "block";
		/* if(jsInterface){
			jsInterface.setNumberOfPages(pages.length);
		} */
	
		document.getElementById('kb-footer-pre').addEventListener('click',
				function() {
					showPreviousPage();
				});
	
		document.getElementById('kb-footer-next').addEventListener('click',
				function() {
					showNextPage()
				});
	
	}, false);
	
	function showPreviousPage() {
		if (currentPage > 0) {
			pages[currentPage].style.display = "none";
			currentPage--;
			pages[currentPage].style.display = "block";
			document.getElementById('kb-footer-label').textContent = (currentPage + 1)
					+ "/" + pages.length
		}
	}
	
	function showNextPage() {
		if (currentPage < pages.length - 1) {
			pages[currentPage].style.display = "none";
			currentPage++;
			pages[currentPage].style.display = "block";
			document.getElementById('kb-footer-label').textContent = (currentPage + 1)
					+ "/" + pages.length
		}
	}
